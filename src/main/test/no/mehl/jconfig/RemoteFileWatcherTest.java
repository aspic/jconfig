package no.mehl.jconfig;

import no.mehl.jconfig.pojo.Config;
import no.mehl.jconfig.updater.RemoteFileWatcher;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertTrue;


public class RemoteFileWatcherTest {

    // TODO: embedded jetty test

    @Test
    public void run_withValidEndpoint_shouldRetrieveConfig() {

        final AtomicBoolean changed = new AtomicBoolean(false);
        ConfigChangeListener listener = new ConfigChangeListener() {
            @Override
            public void configChanged(Config newConfig) {
                System.out.println(newConfig.get("local"));
                changed.set(true);
            }
        };
        RemoteFileWatcher watcher = new RemoteFileWatcher("http://mehl.no/test.json", listener);
        watcher.run();

        assertTrue(changed.get());
    }
}
