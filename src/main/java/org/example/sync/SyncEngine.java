package org.example.sync;

import org.example.config.AppConfig;
import org.example.metadata.MetadataStore;
import org.example.model.FileMetadata;
import org.example.model.SyncAction;
import org.example.model.SyncStats;
import org.example.util.ChecksumUtil;
import org.example.watcher.DirectoryWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.BlockingQueue;

import static java.nio.file.StandardWatchEventKinds.*;

public class SyncEngine implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(SyncEngine.class);

    private final Path sourceRoot;
    private final Path targetRoot;
    private final BlockingQueue<DirectoryWatcher.FileEvent> eventQueue;
    private final MetadataStore metadataStore;
    private final TransferManager transferManager;
    private final ConflictResolver conflictResolver;
    private final AppConfig config;
    private final SyncStats stats;
    private volatile boolean running;

    public SyncEngine(Path sourceRoot, Path targetRoot,
                      BlockingQueue<DirectoryWatcher.FileEvent> eventQueue,
                      MetadataStore metadataStore, TransferManager transferManager,
                      ConflictResolver conflictResolver, AppConfig config, SyncStats stats) {
        this.sourceRoot = sourceRoot;
        this.targetRoot = targetRoot;
        this.eventQueue = eventQueue;
        this.metadataStore = metadataStore;
        this.transferManager = transferManager;
        this.conflictResolver = conflictResolver;
        this.config = config;
        this.stats = stats;
        this.running = false;
    }

    public void start() {
        running = true;
        Thread syncThread = new Thread(this, "SyncEngine");
        syncThread.setDaemon(true);
        syncThread.start();
        logger.info("Sync engine started");
    }

    public void stop() {
        running = false;
        logger.info("Sync engine stopped");
    }

    @Override
    public void run() {
        while (running) {
            try {
                DirectoryWatcher.FileEvent event = eventQueue.take();
                processEvent(event);
            } catch (InterruptedException e) {
                logger.info("Sync engine interrupted");
                break;
            } catch (Exception e) {
                logger.error("Error processing event", e);
                stats.incrementErrors();
            }
        }
    }

    private void processEvent(DirectoryWatcher.FileEvent event) throws IOException {
        Path sourcePath = event.getPath();

        // Skip if it's a directory
        if (Files.isDirectory(sourcePath)) {
            return;
        }

        String relativePath = sourceRoot.relativize(sourcePath).toString();
        Path targetPath = targetRoot.resolve(relativePath);

        if (event.getKind() == ENTRY_DELETE) {
            handleDelete(relativePath, targetPath);
        } else if (event.getKind() == ENTRY_CREATE || event.getKind() == ENTRY_MODIFY) {
            handleCreateOrModify(sourcePath, targetPath, relativePath);
        }
    }

    private void handleDelete(String relativePath, Path targetPath) throws IOException {
        logger.info("Processing DELETE: {}", relativePath);

        if (Files.exists(targetPath)) {
            transferManager.deleteFile(targetPath);
        }

        metadataStore.delete(relativePath);

        SyncAction action = new SyncAction(SyncAction.ActionType.DELETE, relativePath,
                targetPath.toString(), "Source file deleted");
        logger.info("Action: {}", action);
    }

    private void handleCreateOrModify(Path sourcePath, Path targetPath, String relativePath) throws IOException {
        logger.info("Processing CREATE/MODIFY: {}", relativePath);

        // Get current file metadata
        long size = Files.size(sourcePath);
        long mtime = Files.getLastModifiedTime(sourcePath).toMillis();

        // Get stored metadata
        FileMetadata storedMetadata = metadataStore.get(relativePath);

        // Quick check: if size and mtime match, skip
        if (storedMetadata != null &&
                storedMetadata.getSize() == size &&
                storedMetadata.getMtime() == mtime) {
            logger.debug("File unchanged (by size/mtime): {}", relativePath);
            return;
        }

        // Calculate checksum
        String checksum = ChecksumUtil.calculateChecksum(sourcePath,
                config.getSync().getChecksumAlgorithm(),
                config.getSync().getChunkSize());

        // Check if checksum matches
        if (storedMetadata != null && checksum.equals(storedMetadata.getChecksum())) {
            logger.debug("File unchanged (by checksum): {}", relativePath);
            return;
        }

        // Decide action
        SyncAction.ActionType actionType;
        Path finalTarget = targetPath;

        if (!Files.exists(targetPath)) {
            actionType = SyncAction.ActionType.COPY;
        } else {
            // File exists - check for conflict
            long targetMtime = Files.getLastModifiedTime(targetPath).toMillis();

            if (storedMetadata != null && targetMtime > storedMetadata.getLastSyncedAt()) {
                // Conflict: both source and target modified
                logger.warn("Conflict detected: {}", relativePath);
                stats.incrementConflicts();

                finalTarget = conflictResolver.resolveConflict(sourcePath, targetPath);
                if (finalTarget == null) {
                    logger.info("Skipping file due to conflict policy: {}", relativePath);
                    return;
                }
                actionType = SyncAction.ActionType.CONFLICT;
            } else {
                actionType = SyncAction.ActionType.COPY;
            }
        }

        // Perform transfer
        Path targetToCopy = finalTarget;
        transferManager.copyFileAsync(sourcePath, targetToCopy, () -> {
            try {
                // Update metadata
                FileMetadata newMetadata = new FileMetadata(relativePath, size, mtime, checksum);
                metadataStore.save(newMetadata);
            } catch (IOException e) {
                logger.error("Failed to save metadata for: {}", relativePath, e);
            }
        });

        SyncAction action = new SyncAction(actionType, sourcePath.toString(),
                finalTarget.toString(), "File synced");
        logger.info("Action: {}", action);
    }
}