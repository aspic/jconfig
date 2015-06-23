package no.mehl.jconfig.updater;

import no.mehl.jconfig.ConfigChangeListener;
import no.mehl.jconfig.ConfigException;
import no.mehl.jconfig.ConfigParser;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

/**
 * Synchronizes config with a local file
 */
public class FileWatcher implements Runnable {

    private WatchKey watchKey;
    private ConfigChangeListener listener;
    private ConfigParser parser = new ConfigParser();
    private Path configPath;

    public FileWatcher(String directory, String fileName, ConfigChangeListener listener) {
        Path path = Paths.get(directory);
        this.listener = listener;
        this.configPath = Paths.get(directory, fileName);

        try {
            WatchService watchService = FileSystems.getDefault().newWatchService();
            this.watchKey = path.register(watchService, ENTRY_CREATE,ENTRY_DELETE,ENTRY_MODIFY);
        } catch (IOException e) {
            throw new ConfigException(String.format("Unable to setup file watcher for directory=%s and file=%s", directory, fileName), e);
        }
    }

    @Override
    public void run() {
        List<WatchEvent<?>> events = watchKey.pollEvents();
        for(WatchEvent event : events) {
            if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY && event.context().toString().equals(configPath.getFileName().toString())) {
                listener.configChanged(parser.parseFile(configPath));
            }
        }
    }
}
