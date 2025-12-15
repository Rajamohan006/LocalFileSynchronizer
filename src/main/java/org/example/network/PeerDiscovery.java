package org.example.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class PeerDiscovery implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(PeerDiscovery.class);
    private static final int DISCOVERY_PORT = 9877;
    private static final int ANNOUNCEMENT_INTERVAL_MS = 30000; // 30 seconds
    private static final int PEER_TIMEOUT_MS = 90000; // 90 seconds (3x announcement)

    private final String myPeerId;
    private final String myPeerName;
    private final int myServerPort;
    private final ConcurrentHashMap<String, PeerInfo> discoveredPeers;
    private final AtomicBoolean running;

    private DatagramSocket socket;
    private Thread announcerThread;
    private Thread listenerThread;
    private Thread cleanupThread;

    public PeerDiscovery(String peerName, int serverPort) {
        this.myPeerId = UUID.randomUUID().toString();
        this.myPeerName = peerName;
        this.myServerPort = serverPort;
        this.discoveredPeers = new ConcurrentHashMap<>();
        this.running = new AtomicBoolean(false);
    }

    public void start() throws IOException {
        socket = new DatagramSocket(DISCOVERY_PORT);
        socket.setBroadcast(true);
        running.set(true);

        // Start announcer thread
        announcerThread = new Thread(this::announcePresence, "PeerAnnouncer");
        announcerThread.setDaemon(true);
        announcerThread.start();

        // Start listener thread
        listenerThread = new Thread(this::listenForPeers, "PeerListener");
        listenerThread.setDaemon(true);
        listenerThread.start();

        // Start cleanup thread
        cleanupThread = new Thread(this::cleanupStaleConnections, "PeerCleanup");
        cleanupThread.setDaemon(true);
        cleanupThread.start();

        logger.info("Peer discovery started. My ID: {}, Port: {}", myPeerId.substring(0, 8), myServerPort);
    }

    public void stop() {
        running.set(false);
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        logger.info("Peer discovery stopped");
    }

    @Override
    public void run() {
        // Main thread coordination
        while (running.get()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private void announcePresence() {
        while (running.get()) {
            try {
                String message = String.format("PEER_ANNOUNCE|%s|%s|%d", myPeerId, myPeerName, myServerPort);
                byte[] buffer = message.getBytes();

                // Broadcast to all network interfaces
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length,
                        InetAddress.getByName("255.255.255.255"), DISCOVERY_PORT);
                socket.send(packet);

                logger.debug("Announced presence: {}", myPeerName);

                Thread.sleep(ANNOUNCEMENT_INTERVAL_MS);
            } catch (IOException e) {
                logger.error("Failed to announce presence", e);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private void listenForPeers() {
        byte[] buffer = new byte[1024];

        while (running.get()) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String message = new String(packet.getData(), 0, packet.getLength());
                String senderIp = packet.getAddress().getHostAddress();

                handleDiscoveryMessage(message, senderIp);

            } catch (SocketException e) {
                if (running.get()) {
                    logger.error("Socket error while listening", e);
                }
            } catch (IOException e) {
                logger.error("Error receiving discovery packet", e);
            }
        }
    }

    private void handleDiscoveryMessage(String message, String senderIp) {
        if (!message.startsWith("PEER_ANNOUNCE|")) {
            return;
        }

        try {
            String[] parts = message.split("\\|");
            if (parts.length != 4) {
                return;
            }

            String peerId = parts[1];
            String peerName = parts[2];
            int peerPort = Integer.parseInt(parts[3]);

            // Don't add ourselves
            if (peerId.equals(myPeerId)) {
                return;
            }

            // Add or update peer
            PeerInfo peer = discoveredPeers.get(peerId);
            if (peer == null) {
                peer = new PeerInfo(peerId, peerName, senderIp, peerPort);
                discoveredPeers.put(peerId, peer);
                logger.info("Discovered new peer: {}", peer);
            } else {
                peer.setLastSeen(System.currentTimeMillis());
                peer.setOnline(true);
                logger.debug("Updated peer: {}", peerName);
            }

        } catch (Exception e) {
            logger.error("Failed to parse discovery message: {}", message, e);
        }
    }

    private void cleanupStaleConnections() {
        while (running.get()) {
            try {
                long now = System.currentTimeMillis();

                discoveredPeers.values().forEach(peer -> {
                    if (now - peer.getLastSeen() > PEER_TIMEOUT_MS) {
                        if (peer.isOnline()) {
                            peer.setOnline(false);
                            logger.info("Peer went offline: {}", peer.getPeerName());
                        }
                    }
                });

                Thread.sleep(30000); // Check every 30 seconds
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    public ConcurrentHashMap<String, PeerInfo> getDiscoveredPeers() {
        return discoveredPeers;
    }

    public String getMyPeerId() {
        return myPeerId;
    }

    public String getMyPeerName() {
        return myPeerName;
    }
}