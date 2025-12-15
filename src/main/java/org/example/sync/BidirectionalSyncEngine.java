package org.example.sync;

import org.example.metadata.MetadataStore;
import org.example.model.FileMetadata;
import org.example.model.SyncStats;
import org.example.network.NetworkSyncClient;
import org.example.network.PeerDiscovery;
import org.example.network.PeerInfo;
import org.example.util.ChecksumUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BidirectionalSyncEngine {
    private static final Logger logger = LoggerFactory.getLogger(BidirectionalSyncEngine.class);

    private final Path sharedDirectory;
    private final MetadataStore metadataStore;
    private final PeerDiscovery peerDiscovery;
    private final NetworkSyncClient syncClient;
    private final SyncStats stats;
    private final String checksumAlgorithm;
    private final int chunkSize;
    private final ScheduledExecutorService scheduler;
    private volatile boolean running;

    public BidirectionalSyncEngine(Path sharedDirectory, MetadataStore metadataStore,
                                   PeerDiscovery peerDiscovery, SyncStats stats,
                                   String checksumAlgorithm, int chunkSize) {
        this.sharedDirectory = sharedDirectory;
        this.metadataStore = metadataStore;
        this.peerDiscovery = peerDiscovery;
        this.syncClient = new NetworkSyncClient();
        this.stats = stats;
        this.checksumAlgorithm = checksumAlgorithm;
        this.chunkSize = chunkSize;
        this.scheduler = Executors.newScheduledThreadPool(2);
        this.running = false;
    }

    public void start() {
        running = true;
        syncWithAllPeers();
        scheduler.scheduleAtFixedRate(this::syncWithAllPeers, 5, 5, TimeUnit.MINUTES);
        logger.info("Bidirectional sync engine started");
    }

    public void stop() {
        running = false;
        scheduler.shutdown();
        try {
            scheduler.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
        logger.info("Bidirectional sync engine stopped");
    }

    public void syncWithAllPeers() {
        if (!running) return;

        Map<String, PeerInfo> peers = peerDiscovery.getDiscoveredPeers();
        if (peers.isEmpty()) {
            logger.info("No peers online - working in local mode");
            return;
        }

        logger.info("Syncing with {} peer(s)...", peers.size());
        for (PeerInfo peer : peers.values()) {
            if (!peer.isOnline()) continue;
            try {
                syncWithPeer(peer);
            } catch (Exception e) {
                logger.error("Failed to sync with peer: {}", peer.getPeerName(), e);
                stats.incrementErrors();
            }
        }
        logger.info("Sync cycle completed");
    }

    private void syncWithPeer(PeerInfo peer) throws IOException {
        logger.info("Syncing with peer: {}", peer.getPeerName());

        if (!syncClient.ping(peer)) {
            logger.warn("Peer not responding: {}", peer.getPeerName());
            peer.setOnline(false);
            return;
        }

        List<FileMetadata> peerFiles = syncClient.getFileList(peer);
        logger.info("Peer {} has {} files", peer.getPeerName(), peerFiles.size());

        List<FileMetadata> localFiles = metadataStore.getAll();
        Map<String, FileMetadata> peerFileMap = new HashMap<>();
        for (FileMetadata fm : peerFiles) {
            peerFileMap.put(fm.getRelativePath(), fm);
        }

        Map<String, FileMetadata> localFileMap = new HashMap<>();
        for (FileMetadata fm : localFiles) {
            localFileMap.put(fm.getRelativePath(), fm);
        }

        int downloaded = 0;
        int skipped = 0;

        for (FileMetadata peerFile : peerFiles) {
            String relativePath = peerFile.getRelativePath();
            FileMetadata localFile = localFileMap.get(relativePath);

            if (localFile == null) {
                downloadFromPeer(peer, peerFile);
                downloaded++;
            } else {
                if (shouldDownload(localFile, peerFile)) {
                    downloadFromPeer(peer, peerFile);
                    downloaded++;
                } else {
                    skipped++;
                }
            }
        }

        logger.info("Sync with {} complete: {} downloaded, {} skipped",
                peer.getPeerName(), downloaded, skipped);

        // Show notification
        if (downloaded > 0) {
            showNotification("Received " + downloaded + " file(s) from " + peer.getPeerName());
        }
    }

    private boolean shouldDownload(FileMetadata localFile, FileMetadata peerFile) {
        if (peerFile.getMtime() > localFile.getMtime()) {
            return true;
        } else if (peerFile.getMtime() == localFile.getMtime()) {
            if (!peerFile.getChecksum().equals(localFile.getChecksum())) {
                logger.warn("Checksum mismatch for {}", localFile.getRelativePath());
                return true;
            }
        }
        return false;
    }

    private void downloadFromPeer(PeerInfo peer, FileMetadata peerFile) {
        try {
            String relativePath = peerFile.getRelativePath();
            Path targetPath = sharedDirectory.resolve(relativePath);

            syncClient.downloadFile(peer, relativePath, targetPath);

            FileMetadata newMetadata = new FileMetadata();
            newMetadata.setRelativePath(relativePath);
            newMetadata.setSize(Files.size(targetPath));
            newMetadata.setMtime(Files.getLastModifiedTime(targetPath).toMillis());

            String checksum = ChecksumUtil.calculateChecksum(targetPath, checksumAlgorithm, chunkSize);
            newMetadata.setChecksum(checksum);
            newMetadata.setVersion(peerFile.getVersion() + 1);

            metadataStore.save(newMetadata);
            stats.incrementFilesSynced();
            stats.addBytesCopied(newMetadata.getSize());

            logger.info("Downloaded and saved: {}", relativePath);

        } catch (IOException e) {
            logger.error("Failed to download file: {}", peerFile.getRelativePath(), e);
            stats.incrementErrors();
        }
    }

    public void triggerManualSync() {
        logger.info("Manual sync triggered");
        scheduler.execute(this::syncWithAllPeers);
    }

    private void showNotification(String message) {
        System.out.println("\n🔔 NOTIFICATION: " + message);
        logger.info("NOTIFICATION: {}", message);
    }
}
