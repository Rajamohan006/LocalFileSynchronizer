package org.example.watcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static java.nio.file.StandardWatchEventKinds.*;

public class DirectoryWatcher implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(DirectoryWatcher.class);

    private final Path rootPath;
    private final BlockingQueue<FileEvent> eventQueue;
    private final WatchService watchService;
    private final Map<WatchKey, Path> keyPathMap;
    private volatile boolean running;

    public DirectoryWatcher(Path rootPath) throws IOException {
        this.rootPath = rootPath;
        this.eventQueue = new LinkedBlockingQueue<>();
        this.watchService = FileSystems.getDefault().newWatchService();
        this.keyPathMap = new HashMap<>();
        this.running = false;
    }

    public void start() throws IOException {
        registerRecursive(rootPath);
        running = true;
        Thread watcherThread = new Thread(this, "DirectoryWatcher");
        watcherThread.setDaemon(true);
        watcherThread.start();
        logger.info("Directory watcher started for: {}", rootPath);
    }

    public void stop() {
        running = false;
        try {
            watchService.close();
        } catch (IOException e) {
            logger.error("Error closing watch service", e);
        }
        logger.info("Directory watcher stopped");
    }

    @Override
    public void run() {
        while (running) {
            WatchKey key;
            try {
                key = watchService.take();
            } catch (InterruptedException e) {
                logger.info("Watcher interrupted");
                break;
            } catch (ClosedWatchServiceException e) {
                logger.info("Watch service closed");
                break;
            }

            Path dir = keyPathMap.get(key);
            if (dir == null) {
                logger.warn("WatchKey not recognized");
                continue;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();

                if (kind == OVERFLOW) {
                    logger.warn("Overflow event - some events may have been lost");
                    continue;
                }

                @SuppressWarnings("unchecked")
                WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
                Path fileName = pathEvent.context();
                Path fullPath = dir.resolve(fileName);

                // Emit event
                FileEvent fileEvent = new FileEvent(fullPath, kind);
                eventQueue.offer(fileEvent);
                logger.debug("File event: {} - {}", kind.name(), fullPath);

                // If new directory created, register it
                if (kind == ENTRY_CREATE && Files.isDirectory(fullPath)) {
                    try {
                        registerRecursive(fullPath);
                    } catch (IOException e) {
                        logger.error("Failed to register new directory: {}", fullPath, e);
                    }
                }
            }

            boolean valid = key.reset();
            if (!valid) {
                keyPathMap.remove(key);
                if (keyPathMap.isEmpty()) {
                    break;
                }
            }
        }
    }

    private void registerRecursive(Path start) throws IOException {
        Files.walk(start)
                .filter(Files::isDirectory)
                .forEach(path -> {
                    try {
                        WatchKey key = path.register(watchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
                        keyPathMap.put(key, path);
                        logger.debug("Registered directory: {}", path);
                    } catch (IOException e) {
                        logger.error("Failed to register: {}", path, e);
                    }
                });
    }

    public BlockingQueue<FileEvent> getEventQueue() {
        return eventQueue;
    }

    public static class FileEvent {
        private final Path path;
        private final WatchEvent.Kind<?> kind;
        private final long timestamp;

        public FileEvent(Path path, WatchEvent.Kind<?> kind) {
            this.path = path;
            this.kind = kind;
            this.timestamp = System.currentTimeMillis();
        }

        public Path getPath() { return path; }
        public WatchEvent.Kind<?> getKind() { return kind; }
        public long getTimestamp() { return timestamp; }

        @Override
        public String toString() {
            return String.format("FileEvent{kind=%s, path=%s}", kind.name(), path);
        }
    }
}