package no.mehl.jconfig;

import no.mehl.jconfig.pojo.Category;
import no.mehl.jconfig.pojo.Config;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

public class ConfigManagerBuilderTest {

    @Test
    public void withJsonConfig_shouldParseJson() {
        new ConfigManager.ConfiguratorBuilder().withJson("{}").build();
    }

    @Test
    public void withFileConfig_shouldParseJson() {
        ConfigManager configManager = new ConfigManager.ConfiguratorBuilder().withResources("testconfig.json").build();
        assertEquals("bar", configManager.getStringProperty("local", "foo"));
    }

    @Test
    public void withConfig_shouldHaveConfig() {
        Config config = new Config();
        Category env = new Category();
        env.put("foo", "baz");
        config.put("local", env);
        new ConfigManager.ConfiguratorBuilder().withConfig(config).build();
    }

    @Test
    public void withConfigWatcher_shouldReadConfig() throws IOException {
        Path f = Files.createTempFile("config", ".test");

        Files.write(f, "{ \"local\": {\"foo\": \"faz\"}}".getBytes());
        f.toFile().deleteOnExit();
        final AtomicBoolean failed = new AtomicBoolean(true);

        final ConfigManager configManager = new ConfigManager.ConfiguratorBuilder().withFileWatcher("/tmp", f.getFileName().toString(), 1, TimeUnit.SECONDS).build();
        ConfigChangeListener listener = new ConfigChangeListener() {
            @Override
            public void configChanged(Config newConfig) {
                configManager.getPool().shutdown();
                failed.set(false);
            }
        };
        configManager.addConfigChangedListener(listener);
        Files.write(f, "{ \"local\": {\"foo\": \"farr\"}}".getBytes());

        try {
            configManager.getPool().awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertFalse(failed.get());
    }

}