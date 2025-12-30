CREATE DATABASE IF NOT EXISTS minecraft_discord;
USE minecraft_discord;

CREATE TABLE IF NOT EXISTS linked_accounts (
    id INT AUTO_INCREMENT PRIMARY KEY,
    minecraft_uuid VARCHAR(36) NOT NULL UNIQUE,
    discord_id VARCHAR(20) NOT NULL UNIQUE,
    linked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_minecraft_uuid (minecraft_uuid),
    INDEX idx_discord_id (discord_id)
);

CREATE TABLE IF NOT EXISTS pending_verifications (
    id INT AUTO_INCREMENT PRIMARY KEY,
    minecraft_uuid VARCHAR(36) NOT NULL UNIQUE,
    minecraft_username VARCHAR(16) NOT NULL,
    verification_code VARCHAR(6) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    INDEX idx_verification_code (verification_code),
    INDEX idx_expires_at (expires_at)
);
