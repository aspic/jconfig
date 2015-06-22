package no.mehl.jconfig;

import no.mehl.jconfig.pojo.Config;
import no.mehl.jconfig.pojo.Environment;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileAttribute;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

public class ConfiguratorBuilderTest {

    @Test
    public void withJsonConfig_shouldParseJson() {
        new Configurator.ConfiguratorBuilder().withJsonConfig("{}").build();
    }

    @Test
    public void withFileConfig_shouldParseJson() {
        Configurator configurator = new Configurator.ConfiguratorBuilder().withFileConfig("testconfig.json").build();
        assertEquals("bar", configurator.getStringProperty("local", "foo"));
    }

    @Test
    public void withConfig_shouldHaveConfig() {
        Config config = new Config();
        Environment env = new Environment();
        env.put("foo", "baz");
        config.put("local", env);
        new Configurator.ConfiguratorBuilder().withConfig(config).build();
    }

    @Test
    public void withConfigWatcher_shouldReadConfig() throws IOException {
        Path f = Files.createTempFile("config", ".test");

        Files.write(f, "{ \"local\": {\"foo\": \"faz\"}}".getBytes());
        f.toFile().deleteOnExit();
        final AtomicBoolean failed = new AtomicBoolean(true);

        final Configurator configurator = new Configurator.ConfiguratorBuilder().withConfigWatcher("/tmp", f.getFileName().toString(), 1, TimeUnit.SECONDS).build();
        ConfigChangeListener listener = new ConfigChangeListener() {
            @Override
            public void configChanged(Config newConfig) {
                configurator.getPool().shutdown();
                failed.set(false);
            }
        };
        configurator.addConfigChangedListener(listener);
        Files.write(f, "{ \"local\": {\"foo\": \"farr\"}}".getBytes());

        try {
            configurator.getPool().awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertFalse(failed.get());
    }

}