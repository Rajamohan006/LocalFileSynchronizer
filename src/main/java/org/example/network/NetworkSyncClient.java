package org.example.network;

import org.example.model.FileMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class NetworkSyncClient {
    private static final Logger logger = LoggerFactory.getLogger(NetworkSyncClient.class);
    private static final int TIMEOUT_MS = 5000;

    public boolean ping(PeerInfo peer) {
        try {
            URL url = new URL(peer.getEndpoint() + "/api/ping");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);

            int responseCode = conn.getResponseCode();
            conn.disconnect();

            return responseCode == 200;
        } catch (Exception e) {
            logger.debug("Ping failed for {}: {}", peer.getPeerName(), e.getMessage());
            return false;
        }
    }

    public List<FileMetadata> getFileList(PeerInfo peer) throws IOException {
        URL url = new URL(peer.getEndpoint() + "/api/files/list");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(TIMEOUT_MS);
        conn.setReadTimeout(TIMEOUT_MS);

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            conn.disconnect();
            throw new IOException("Failed to get file list: HTTP " + responseCode);
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        conn.disconnect();

        return parseFileListJson(response.toString());
    }

    public void downloadFile(PeerInfo peer, String relativePath, Path targetPath) throws IOException {
        String encodedPath = URLEncoder.encode(relativePath, StandardCharsets.UTF_8);
        URL url = new URL(peer.getEndpoint() + "/api/files/download?path=" + encodedPath);

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(TIMEOUT_MS);
        conn.setReadTimeout(30000);

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            conn.disconnect();
            throw new IOException("Failed to download file: HTTP " + responseCode);
        }

        Files.createDirectories(targetPath.getParent());
        Path tempFile = targetPath.getParent().resolve(targetPath.getFileName() + ".tmp");

        try (InputStream in = conn.getInputStream();
             OutputStream out = Files.newOutputStream(tempFile)) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }

        conn.disconnect();
        Files.move(tempFile, targetPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        logger.info("Downloaded: {} from {}", relativePath, peer.getPeerName());
    }

    private List<FileMetadata> parseFileListJson(String json) {
        List<FileMetadata> result = new ArrayList<>();
        json = json.trim();
        if (!json.startsWith("[") || !json.endsWith("]")) {
            return result;
        }

        json = json.substring(1, json.length() - 1);
        String[] objects = json.split("\\},\\{");

        for (String obj : objects) {
            obj = obj.replace("{", "").replace("}", "");
            FileMetadata fm = new FileMetadata();
            String[] fields = obj.split(",");

            for (String field : fields) {
                String[] kv = field.split(":", 2);
                if (kv.length != 2) continue;

                String key = kv[0].replace("\"", "").trim();
                String value = kv[1].replace("\"", "").trim();

                switch (key) {
                    case "path" -> fm.setRelativePath(value);
                    case "size" -> fm.setSize(Long.parseLong(value));
                    case "mtime" -> fm.setMtime(Long.parseLong(value));
                    case "checksum" -> fm.setChecksum(value);
                    case "version" -> fm.setVersion(Integer.parseInt(value));
                }
            }

            if (fm.getRelativePath() != null) {
                result.add(fm);
            }
        }

        return result;
    }
}
