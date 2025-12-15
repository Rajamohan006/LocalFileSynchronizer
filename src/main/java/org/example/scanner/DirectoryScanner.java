package org.example.scanner;


import org.example.watcher.DirectoryWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.BlockingQueue;
import java.util.regex.Pattern;
import java.util.List;

import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

public class DirectoryScanner implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(DirectoryScanner.class);

    private final Path rootPath;
    private final BlockingQueue<DirectoryWatcher.FileEvent> eventQueue;
    private final List<Pattern> excludePatterns;
    private final int scanIntervalSeconds;
    private volatile boolean running;

    public DirectoryScanner(Path rootPath, BlockingQueue<DirectoryWatcher.FileEvent> eventQueue,
                            List<String> excludePatterns, int scanIntervalSeconds) {
        this.rootPath = rootPath;
        this.eventQueue = eventQueue;
        this.excludePatterns = excludePatterns.stream()
                .map(Pattern::compile)
                .toList();
        this.scanIntervalSeconds = scanIntervalSeconds;
        this.running = false;
    }

    public void start() {
        running = true;
        Thread scannerThread = new Thread(this, "DirectoryScanner");
        scannerThread.setDaemon(true);
        scannerThread.start();
        logger.info("Directory scanner started, interval: {}s", scanIntervalSeconds);
    }

    public void stop() {
        running = false;
        logger.info("Directory scanner stopped");
    }

    @Override
    public void run() {
        while (running) {
            try {
                logger.info("Starting periodic directory scan...");
                scanDirectory();
                logger.info("Periodic scan completed");

                Thread.sleep(scanIntervalSeconds * 1000L);
            } catch (InterruptedException e) {
                logger.info("Scanner interrupted");
                break;
            } catch (IOException e) {
                logger.error("Error during directory scan", e);
            }
        }
    }

    private void scanDirectory() throws IOException {
        Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (shouldExclude(file)) {
                    return FileVisitResult.CONTINUE;
                }

                // Queue as MODIFY event for sync engine to process
                DirectoryWatcher.FileEvent event = new DirectoryWatcher.FileEvent(file, ENTRY_MODIFY);
                eventQueue.offer(event);

                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                logger.warn("Failed to visit file: {}", file, exc);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private boolean shouldExclude(Path path) {
        String pathStr = path.toString();
        return excludePatterns.stream().anyMatch(pattern -> pattern.matcher(pathStr).matches());
    }
}