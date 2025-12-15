package org.example.network;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.example.metadata.MetadataStore;
import org.example.model.FileMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class NetworkSyncServer {
    private static final Logger logger = LoggerFactory.getLogger(NetworkSyncServer.class);

    private final int port;
    private final Path sharedDirectory;
    private final MetadataStore metadataStore;
    private HttpServer server;

    public NetworkSyncServer(int port, Path sharedDirectory, MetadataStore metadataStore) {
        this.port = port;
        this.sharedDirectory = sharedDirectory;
        this.metadataStore = metadataStore;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);

        // API endpoints
        server.createContext("/api/ping", this::handlePing);
        server.createContext("/api/files/list", this::handleFileList);
        server.createContext("/api/files/download", this::handleFileDownload);
        server.createContext("/api/files/upload", this::handleFileUpload);

        server.setExecutor(null); // Use default executor
        server.start();

        logger.info("Network sync server started on port {}", port);
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            logger.info("Network sync server stopped");
        }
    }

    private void handlePing(HttpExchange exchange) throws IOException {
        String response = "{\"status\":\"ok\",\"message\":\"pong\"}";
        sendResponse(exchange, 200, response);
    }

    private void handleFileList(HttpExchange exchange) throws IOException {
        try {
            List<FileMetadata> allFiles = metadataStore.getAll();

            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < allFiles.size(); i++) {
                FileMetadata fm = allFiles.get(i);
                json.append(String.format(
                        "{\"path\":\"%s\",\"size\":%d,\"mtime\":%d,\"checksum\":\"%s\",\"version\":%d}",
                        fm.getRelativePath(), fm.getSize(), fm.getMtime(),
                        fm.getChecksum(), fm.getVersion()
                ));
                if (i < allFiles.size() - 1) {
                    json.append(",");
                }
            }
            json.append("]");

            sendResponse(exchange, 200, json.toString());
            logger.debug("Sent file list: {} files", allFiles.size());

        } catch (Exception e) {
            logger.error("Error getting file list", e);
            sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    private void handleFileDownload(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        String relativePath = getQueryParam(query, "path");

        if (relativePath == null) {
            sendResponse(exchange, 400, "{\"error\":\"Missing path parameter\"}");
            return;
        }

        Path filePath = sharedDirectory.resolve(relativePath);

        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            sendResponse(exchange, 404, "{\"error\":\"File not found\"}");
            return;
        }

        try {
            byte[] fileBytes = Files.readAllBytes(filePath);
            exchange.sendResponseHeaders(200, fileBytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(fileBytes);
            os.close();

            logger.info("Sent file: {}", relativePath);
        } catch (IOException e) {
            logger.error("Error sending file: {}", relativePath, e);
            throw e;
        }
    }

    private void handleFileUpload(HttpExchange exchange) throws IOException {
        sendResponse(exchange, 501, "{\"error\":\"Upload via POST - use client instead\"}");
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        byte[] bytes = response.getBytes();
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }

    private String getQueryParam(String query, String param) {
        if (query == null) return null;
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=");
            if (kv.length == 2 && kv[0].equals(param)) {
                return kv[1];
            }
        }
        return null;
    }
}