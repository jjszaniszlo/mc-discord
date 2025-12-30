package com.jjszaniszlo.discordIntegration.database;

import com.jjszaniszlo.discordIntegration.config.ModConfig;
import com.jjszaniszlo.discordIntegration.verification.VerificationCode;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public class DatabaseManager {
    private static DatabaseManager instance;
    private Connection connection;
    private final ModConfig.MySqlConfig config;

    public record PendingVerification(UUID minecraftUuid, String minecraftUsername) {}

    private static final String CREATE_LINKED_ACCOUNTS_TABLE = """
        CREATE TABLE IF NOT EXISTS linked_accounts (
            id INT AUTO_INCREMENT PRIMARY KEY,
            minecraft_uuid VARCHAR(36) NOT NULL UNIQUE,
            discord_id VARCHAR(20) NOT NULL UNIQUE,
            linked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            INDEX idx_minecraft_uuid (minecraft_uuid),
            INDEX idx_discord_id (discord_id)
        )
        """;

    private static final String CREATE_PENDING_VERIFICATIONS_TABLE = """
        CREATE TABLE IF NOT EXISTS pending_verifications (
            id INT AUTO_INCREMENT PRIMARY KEY,
            minecraft_uuid VARCHAR(36) NOT NULL UNIQUE,
            minecraft_username VARCHAR(16) NOT NULL,
            verification_code VARCHAR(6) NOT NULL UNIQUE,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            expires_at TIMESTAMP NOT NULL,
            INDEX idx_verification_code (verification_code),
            INDEX idx_expires_at (expires_at)
        )
        """;

    private DatabaseManager(ModConfig.MySqlConfig config) {
        this.config = config;
    }

    public static DatabaseManager getInstance() {
        return instance;
    }

    public static void initialize(ModConfig.MySqlConfig config) throws SQLException {
        instance = new DatabaseManager(config);
        instance.connect();
        instance.createTables();
    }

    private void connect() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            return;
        }
        connection = DriverManager.getConnection(
            config.getJdbcUrl(),
            config.username,
            config.password
        );
    }

    private void ensureConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connect();
        }
    }

    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(CREATE_LINKED_ACCOUNTS_TABLE);
            stmt.execute(CREATE_PENDING_VERIFICATIONS_TABLE);
        }
    }

    public Optional<String> getLinkedDiscordId(UUID minecraftUuid) throws SQLException {
        ensureConnection();
        String sql = "SELECT discord_id FROM linked_accounts WHERE minecraft_uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, minecraftUuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(rs.getString("discord_id"));
                }
            }
        }
        return Optional.empty();
    }

    public boolean isPlayerLinked(UUID minecraftUuid) throws SQLException {
        return getLinkedDiscordId(minecraftUuid).isPresent();
    }

    public String createPendingVerification(UUID minecraftUuid, String username) throws SQLException {
        ensureConnection();
        int expirationMinutes = ModConfig.getInstance().getCodeExpirationMinutes();

        String checkSql = "SELECT verification_code, expires_at FROM pending_verifications WHERE minecraft_uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(checkSql)) {
            stmt.setString(1, minecraftUuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Timestamp expiresAt = rs.getTimestamp("expires_at");
                    if (expiresAt.toLocalDateTime().isAfter(LocalDateTime.now())) {
                        return rs.getString("verification_code");
                    }
                    deletePendingVerification(minecraftUuid);
                }
            }
        }

        String code = generateUniqueCode();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(expirationMinutes);

        String insertSql = """
            INSERT INTO pending_verifications (minecraft_uuid, minecraft_username, verification_code, expires_at)
            VALUES (?, ?, ?, ?)
            """;
        try (PreparedStatement stmt = connection.prepareStatement(insertSql)) {
            stmt.setString(1, minecraftUuid.toString());
            stmt.setString(2, username);
            stmt.setString(3, code);
            stmt.setTimestamp(4, Timestamp.valueOf(expiresAt));
            stmt.executeUpdate();
        }

        return code;
    }

    private String generateUniqueCode() throws SQLException {
        String checkSql = "SELECT 1 FROM pending_verifications WHERE verification_code = ?";
        for (int i = 0; i < 10; i++) {
            String code = VerificationCode.generate();
            try (PreparedStatement stmt = connection.prepareStatement(checkSql)) {
                stmt.setString(1, code);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        return code;
                    }
                }
            }
        }
        throw new SQLException("Failed to generate unique verification code after 10 attempts");
    }

    private void deletePendingVerification(UUID minecraftUuid) throws SQLException {
        String sql = "DELETE FROM pending_verifications WHERE minecraft_uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, minecraftUuid.toString());
            stmt.executeUpdate();
        }
    }

    public Optional<PendingVerification> verifyCode(String code) throws SQLException {
        ensureConnection();
        String sql = "SELECT minecraft_uuid, minecraft_username, expires_at FROM pending_verifications WHERE verification_code = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, code.toUpperCase());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Timestamp expiresAt = rs.getTimestamp("expires_at");
                    if (expiresAt.toLocalDateTime().isAfter(LocalDateTime.now())) {
                        return Optional.of(new PendingVerification(
                            UUID.fromString(rs.getString("minecraft_uuid")),
                            rs.getString("minecraft_username")
                        ));
                    }
                }
            }
        }
        return Optional.empty();
    }

    public boolean isDiscordIdAlreadyLinked(String discordId) throws SQLException {
        ensureConnection();
        String sql = "SELECT 1 FROM linked_accounts WHERE discord_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, discordId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void linkAccount(UUID minecraftUuid, String discordId) throws SQLException {
        ensureConnection();
        String sql = "INSERT INTO linked_accounts (minecraft_uuid, discord_id) VALUES (?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, minecraftUuid.toString());
            stmt.setString(2, discordId);
            stmt.executeUpdate();
        }
    }

    public void deletePendingVerification(String code) throws SQLException {
        ensureConnection();
        String sql = "DELETE FROM pending_verifications WHERE verification_code = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, code.toUpperCase());
            stmt.executeUpdate();
        }
    }

    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
            }
        }
    }
}
