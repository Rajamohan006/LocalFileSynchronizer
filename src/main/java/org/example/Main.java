package org.example;

import org.example.cli.EnhancedCLIController;
import org.example.config.AppConfig;
import org.example.metadata.JsonMetadataStore;
import org.example.metadata.MetadataStore;
import org.example.metadata.SqliteMetadataStore;
import org.example.model.SyncStats;
import org.example.network.NetworkSyncServer;
import org.example.network.PeerDiscovery;
import org.example.scanner.DirectoryScanner;
import org.example.sync.BidirectionalSyncEngine;
import org.example.sync.ConflictResolver;
import org.example.sync.SyncEngine;
import org.example.sync.TransferManager;
import org.example.watcher.DirectoryWatcher;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        String configPath = args.length > 0 ? args[0] : "config.yaml";

        try {
            // Load configuration
            logger.info("Loading configuration from: {}", configPath);
            AppConfig config = AppConfig.loadFromFile(configPath);
            config.validate();

            // Initialize stats
            SyncStats stats = new SyncStats();

            // Initialize metadata store
            MetadataStore metadataStore = createMetadataStore(config);
            metadataStore.initialize();

            // Source directory (single source for now)
            Path sourceRoot = Paths.get(config.getSourceDirs().get(0));
            Path targetRoot = Paths.get(config.getTargetDir());

            // Ensure directories exist
            if (!Files.exists(sourceRoot)) {
                Files.createDirectories(sourceRoot);
                logger.info("Created source directory: {}", sourceRoot);
            }
            if (!Files.exists(targetRoot)) {
                Files.createDirectories(targetRoot);
                logger.info("Created target directory: {}", targetRoot);
            }

            logger.info("Source: {}", sourceRoot);
            logger.info("Target: {}", targetRoot);

            // Initialize peer discovery
            PeerDiscovery peerDiscovery = new PeerDiscovery(
                    config.getPeer().getName(),
                    config.getPeer().getPort()
            );
            peerDiscovery.start();

            // Initialize network sync server
            NetworkSyncServer syncServer = new NetworkSyncServer(
                    config.getPeer().getPort(),
                    sourceRoot,
                    metadataStore
            );
            syncServer.start();

            // Initialize bidirectional sync engine
            BidirectionalSyncEngine bidirectionalSync = new BidirectionalSyncEngine(
                    sourceRoot,
                    metadataStore,
                    peerDiscovery,
                    stats,
                    config.getSync().getChecksumAlgorithm(),
                    config.getSync().getChunkSize()
            );
            bidirectionalSync.start();

            // Local components (for watching local changes)
            DirectoryWatcher watcher = new DirectoryWatcher(sourceRoot);
            DirectoryScanner scanner = new DirectoryScanner(
                    sourceRoot,
                    watcher.getEventQueue(),
                    config.getExcludePatterns(),
                    config.getSync().getScanIntervalSeconds()
            );

            TransferManager transferManager = new TransferManager(
                    config.getSync().getThreadPoolSize(),
                    config.getSync().getChunkSize(),
                    stats
            );

            ConflictResolver conflictResolver = new ConflictResolver(
                    ConflictResolver.Policy.valueOf(config.getConflictPolicy())
            );

            SyncEngine localSyncEngine = new SyncEngine(
                    sourceRoot, targetRoot,
                    watcher.getEventQueue(),
                    metadataStore, transferManager,
                    conflictResolver, config, stats
            );

            // Start local sync components
            logger.info("Starting local file synchronization...");
            watcher.start();
            scanner.start();
            localSyncEngine.start();

            // Start CLI
            EnhancedCLIController cli = new EnhancedCLIController(stats, peerDiscovery, bidirectionalSync);
            cli.start();

            // Wait for CLI to signal stop
            while (cli.isRunning()) {
                Thread.sleep(1000);
            }

            // Shutdown
            logger.info("Shutting down...");
            bidirectionalSync.stop();
            syncServer.stop();
            peerDiscovery.stop();
            localSyncEngine.stop();
            scanner.stop();
            watcher.stop();
            transferManager.shutdown();
            metadataStore.close();

            logger.info("Shutdown complete. Final stats: {}", stats);
            System.out.println("\n" + stats);
            System.out.println("\nThank you for using Local File Synchronizer!");

        } catch (Exception e) {
            logger.error("Fatal error", e);
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static MetadataStore createMetadataStore(AppConfig config) {
        String type = config.getMetadata().getType();
        String dbPath = config.getMetadata().getDbPath();

        if ("SQLITE".equalsIgnoreCase(type)) {
            logger.info("Using SQLite metadata store");
            return new SqliteMetadataStore(dbPath);
        } else {
            logger.info("Using JSON metadata store");
            return new JsonMetadataStore(dbPath);
        }
    }
}