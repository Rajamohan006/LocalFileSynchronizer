package org.example.sync;

import org.example.model.SyncStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TransferManager {
    private static final Logger logger = LoggerFactory.getLogger(TransferManager.class);

    private final ExecutorService executor;
    private final int chunkSize;
    private final SyncStats stats;

    public TransferManager(int threadPoolSize, int chunkSize, SyncStats stats) {
        this.executor = Executors.newFixedThreadPool(threadPoolSize);
        this.chunkSize = chunkSize;
        this.stats = stats;
    }

    public void copyFileAsync(Path source, Path target, Runnable onComplete) {
        executor.submit(() -> {
            try {
                copyFile(source, target);
                if (onComplete != null) {
                    onComplete.run();
                }
            } catch (IOException e) {
                logger.error("Failed to copy file: {} -> {}", source, target, e);
                stats.incrementErrors();
            }
        });
    }

    public void copyFile(Path source, Path target) throws IOException {
        // Ensure parent directory exists
        Path parentDir = target.getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
        }

        // Create temp file for atomic operation
        Path tempFile = target.getParent().resolve(target.getFileName() + ".tmp");

        try {
            // Chunked copy
            try (InputStream in = Files.newInputStream(source);
                 OutputStream out = Files.newOutputStream(tempFile)) {

                byte[] buffer = new byte[chunkSize];
                int bytesRead;
                long totalBytes = 0;

                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                    totalBytes += bytesRead;
                }

                stats.addBytesCopied(totalBytes);
            }

            // Atomic rename
            Files.move(tempFile, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

            // Preserve timestamps
            Files.setLastModifiedTime(target, Files.getLastModifiedTime(source));

            stats.incrementFilesSynced();
            logger.info("Copied: {} -> {}", source, target);

        } catch (IOException e) {
            // Cleanup temp file on failure
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException cleanupEx) {
                logger.warn("Failed to cleanup temp file: {}", tempFile, cleanupEx);
            }
            throw e;
        }
    }

    public void deleteFile(Path target) throws IOException {
        if (Files.exists(target)) {
            Files.delete(target);
            logger.info("Deleted: {}", target);
        }
    }

    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
            logger.info("Transfer manager shutdown complete");
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}