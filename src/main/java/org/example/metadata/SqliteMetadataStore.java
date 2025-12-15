package org.example.metadata;

import org.example.model.FileMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SqliteMetadataStore implements MetadataStore {
    private static final Logger logger = LoggerFactory.getLogger(SqliteMetadataStore.class);
    private final String dbPath;
    private Connection connection;

    public SqliteMetadataStore(String dbPath) {
        this.dbPath = dbPath;
    }

    @Override
    public void initialize() throws IOException {
        try {
            // Create parent directories if needed
            File dbFile = new File(dbPath);
            File parentDir = dbFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            // Connect to SQLite
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);

            // Create table
            String createTableSQL = """
                CREATE TABLE IF NOT EXISTS files (
                    relative_path TEXT PRIMARY KEY,
                    size INTEGER NOT NULL,
                    mtime INTEGER NOT NULL,
                    checksum TEXT,
                    version INTEGER DEFAULT 1,
                    last_synced INTEGER NOT NULL,
                    status TEXT DEFAULT 'NORMAL'
                )
                """;

            try (Statement stmt = connection.createStatement()) {
                stmt.execute(createTableSQL);
            }

            logger.info("SQLite metadata store initialized at: {}", dbPath);
        } catch (SQLException e) {
            throw new IOException("Failed to initialize SQLite metadata store", e);
        }
    }

    @Override
    public void save(FileMetadata metadata) throws IOException {
        String sql = """
            INSERT OR REPLACE INTO files 
            (relative_path, size, mtime, checksum, version, last_synced, status) 
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, metadata.getRelativePath());
            pstmt.setLong(2, metadata.getSize());
            pstmt.setLong(3, metadata.getMtime());
            pstmt.setString(4, metadata.getChecksum());
            pstmt.setInt(5, metadata.getVersion());
            pstmt.setLong(6, metadata.getLastSyncedAt());
            pstmt.setString(7, metadata.getStatus().name());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new IOException("Failed to save metadata for: " + metadata.getRelativePath(), e);
        }
    }

    @Override
    public FileMetadata get(String relativePath) throws IOException {
        String sql = "SELECT * FROM files WHERE relative_path = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, relativePath);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return extractMetadata(rs);
                }
            }
        } catch (SQLException e) {
            throw new IOException("Failed to get metadata for: " + relativePath, e);
        }

        return null;
    }

    @Override
    public List<FileMetadata> getAll() throws IOException {
        List<FileMetadata> result = new ArrayList<>();
        String sql = "SELECT * FROM files WHERE status != 'DELETED'";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                result.add(extractMetadata(rs));
            }
        } catch (SQLException e) {
            throw new IOException("Failed to get all metadata", e);
        }

        return result;
    }

    @Override
    public void delete(String relativePath) throws IOException {
        String sql = "DELETE FROM files WHERE relative_path = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, relativePath);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new IOException("Failed to delete metadata for: " + relativePath, e);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                logger.info("SQLite metadata store closed");
            }
        } catch (SQLException e) {
            throw new IOException("Failed to close SQLite connection", e);
        }
    }

    private FileMetadata extractMetadata(ResultSet rs) throws SQLException {
        FileMetadata metadata = new FileMetadata();
        metadata.setRelativePath(rs.getString("relative_path"));
        metadata.setSize(rs.getLong("size"));
        metadata.setMtime(rs.getLong("mtime"));
        metadata.setChecksum(rs.getString("checksum"));
        metadata.setVersion(rs.getInt("version"));
        metadata.setLastSyncedAt(rs.getLong("last_synced"));
        metadata.setStatus(FileMetadata.FileStatus.valueOf(rs.getString("status")));
        return metadata;
    }
}
