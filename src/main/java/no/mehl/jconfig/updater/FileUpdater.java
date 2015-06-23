package no.mehl.jconfig.updater;

import no.mehl.jconfig.ConfigChangeListener;
import no.mehl.jconfig.ConfiguratorException;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

public class FileUpdater implements Runnable {

    private WatchKey watchKey;
    private String fileName;
    private ConfigChangeListener listener;

    public FileUpdater(String directory, String fileName, ConfigChangeListener listener) {
        Path path = Paths.get(directory);
        this.listener = listener;
        this.fileName = fileName;

        try {
            WatchService watchService = FileSystems.getDefault().newWatchService();
            this.watchKey = path.register(watchService, ENTRY_CREATE,ENTRY_DELETE,ENTRY_MODIFY);
        } catch (IOException e) {
            throw new ConfiguratorException(String.format("Unable to setup file watcher for directory=%s and file=%s", directory, fileName), e);
        }
    }

    @Override
    public void run() {
        List<WatchEvent<?>> events = watchKey.pollEvents();
        for(WatchEvent event : events) {
            if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY && event.context().toString().equals(fileName)) {
                listener.configChanged(null);
            }
        }
    }
}
