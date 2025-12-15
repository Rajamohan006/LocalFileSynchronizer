package org.example.network;

import java.time.Instant;

public class PeerInfo {
    private String peerId;
    private String peerName;
    private String ipAddress;
    private int port;
    private long lastSeen;
    private boolean isOnline;

    public PeerInfo(String peerId, String peerName, String ipAddress, int port) {
        this.peerId = peerId;
        this.peerName = peerName;
        this.ipAddress = ipAddress;
        this.port = port;
        this.lastSeen = Instant.now().toEpochMilli();
        this.isOnline = true;
    }

    // Getters and Setters
    public String getPeerId() { return peerId; }
    public void setPeerId(String peerId) { this.peerId = peerId; }

    public String getPeerName() { return peerName; }
    public void setPeerName(String peerName) { this.peerName = peerName; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public long getLastSeen() { return lastSeen; }
    public void setLastSeen(long lastSeen) { this.lastSeen = lastSeen; }

    public boolean isOnline() { return isOnline; }
    public void setOnline(boolean online) { isOnline = online; }

    public String getEndpoint() {
        return "http://" + ipAddress + ":" + port;
    }

    @Override
    public String toString() {
        return String.format("Peer{name='%s', id='%s', address=%s:%d, online=%s}",
                peerName, peerId.substring(0, 8), ipAddress, port, isOnline);
    }
}
