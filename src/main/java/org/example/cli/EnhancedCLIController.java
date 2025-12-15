package org.example.cli;

import org.example.model.SyncStats;
import org.example.network.PeerDiscovery;
import org.example.network.PeerInfo;
import org.example.sync.BidirectionalSyncEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Scanner;

public class EnhancedCLIController {
    private static final Logger logger = LoggerFactory.getLogger(EnhancedCLIController.class);
    private final SyncStats stats;
    private final PeerDiscovery peerDiscovery;
    private final BidirectionalSyncEngine syncEngine;
    private volatile boolean running;

    public EnhancedCLIController(SyncStats stats, PeerDiscovery peerDiscovery,
                                 BidirectionalSyncEngine syncEngine) {
        this.stats = stats;
        this.peerDiscovery = peerDiscovery;
        this.syncEngine = syncEngine;
        this.running = true;
    }

    public void start() {
        Thread cliThread = new Thread(this::runCLI, "CLI-Thread");
        cliThread.setDaemon(false);
        cliThread.start();
    }

    private void runCLI() {
        Scanner scanner = new Scanner(System.in);
        printWelcome();

        while (running) {
            System.out.print("\n> ");
            String input = scanner.nextLine().trim().toLowerCase();

            switch (input) {
                case "status" -> showStatus();
                case "stats" -> showStats();
                case "peers" -> showPeers();
                case "sync" -> manualSync();
                case "help" -> showHelp();
                case "stop", "exit", "quit" -> confirmStop(scanner);
                case "" -> {}
                default -> System.out.println("Unknown command: " + input + ". Type 'help' for available commands.");
            }
        }

        scanner.close();
    }

    private void printWelcome() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("    LOCAL FILE SYNCHRONIZER - NETWORK EDITION");
        System.out.println("    Peer-to-Peer Real-Time File Mirroring");
        System.out.println("=".repeat(70));
        System.out.println("\n✓ Local synchronization: ACTIVE");
        System.out.println("✓ Network discovery: ACTIVE");
        System.out.println("✓ Peer-to-peer sync: ACTIVE");
        System.out.println("\nType 'help' for available commands.");
        System.out.println("Type 'peers' to see discovered devices.\n");
    }

    private void showStatus() {
        System.out.println("\n--- SYNC STATUS ---");
        System.out.println("Status: RUNNING");
        System.out.println("Mode: Peer-to-Peer Network Sync");
        System.out.println("Uptime: " + formatUptime(stats.getUptimeSeconds()));
        System.out.println("Files Synced: " + stats.getFilesSynced());

        Map<String, PeerInfo> peers = peerDiscovery.getDiscoveredPeers();
        long onlinePeers = peers.values().stream().filter(PeerInfo::isOnline).count();
        System.out.println("Online Peers: " + onlinePeers + " / " + peers.size());
    }

    private void showStats() {
        System.out.println("\n--- SYNC STATISTICS ---");
        System.out.println("Files Synced: " + stats.getFilesSynced());
        System.out.println("Bytes Copied: " + formatBytes(stats.getBytesCopied()));
        System.out.println("Conflicts: " + stats.getConflicts());
        System.out.println("Errors: " + stats.getErrors());
        System.out.println("Uptime: " + formatUptime(stats.getUptimeSeconds()));
    }

    private void showPeers() {
        System.out.println("\n--- DISCOVERED PEERS ---");

        Map<String, PeerInfo> peers = peerDiscovery.getDiscoveredPeers();

        if (peers.isEmpty()) {
            System.out.println("No peers discovered yet.");
            System.out.println("Make sure other devices are running the application on the same network.");
            return;
        }

        System.out.println("Total peers: " + peers.size());
        System.out.println();

        for (PeerInfo peer : peers.values()) {
            String status = peer.isOnline() ? "✓ ONLINE" : "✗ OFFLINE";
            String statusColor = peer.isOnline() ? "" : " (last seen " +
                    formatLastSeen(peer.getLastSeen()) + " ago)";

            System.out.println(String.format("  %s %s", status, peer.getPeerName()));
            System.out.println(String.format("     Address: %s:%d", peer.getIpAddress(), peer.getPort()));
            System.out.println(String.format("     ID: %s%s", peer.getPeerId().substring(0, 12), statusColor));
            System.out.println();
        }
    }

    private void manualSync() {
        System.out.println("\n▶ Triggering manual sync...");
        syncEngine.triggerManualSync();
        System.out.println("✓ Sync initiated. Check logs for progress.");
    }

    private void showHelp() {
        System.out.println("\n--- AVAILABLE COMMANDS ---");
        System.out.println("  status  - Show synchronization status");
        System.out.println("  stats   - Show detailed statistics");
        System.out.println("  peers   - Show discovered peer devices");
        System.out.println("  sync    - Trigger manual sync with all peers");
        System.out.println("  help    - Show this help message");
        System.out.println("  stop    - Stop synchronization and exit");
    }

    private void confirmStop(Scanner scanner) {
        System.out.print("\nAre you sure you want to stop synchronization? (yes/no): ");
        String confirm = scanner.nextLine().trim().toLowerCase();

        if (confirm.equals("yes") || confirm.equals("y")) {
            System.out.println("\nStopping synchronization...");
            running = false;
        } else {
            System.out.println("Stop cancelled.");
        }
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    private String formatUptime(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, secs);
    }

    private String formatLastSeen(long timestamp) {
        long seconds = (System.currentTimeMillis() - timestamp) / 1000;
        if (seconds < 60) return seconds + "s";
        if (seconds < 3600) return (seconds / 60) + "m";
        return (seconds / 3600) + "h";
    }

    public boolean isRunning() {
        return running;
    }
}
