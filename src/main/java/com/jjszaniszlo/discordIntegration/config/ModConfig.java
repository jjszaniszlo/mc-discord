package com.jjszaniszlo.discordIntegration.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE = "discord-integration.json";

    private static ModConfig instance;

    private MySqlConfig mysql = new MySqlConfig();
    private DiscordConfig discord = new DiscordConfig();
    private int codeExpirationMinutes = 15;
    private String language = "en";

    public static class MySqlConfig {
        public String host = "localhost";
        public int port = 3306;
        public String database = "minecraft_discord";
        public String username = "minecraft";
        public String password = "changeme";

        public String getJdbcUrl() {
            return String.format("jdbc:mysql://%s:%d/%s?connectTimeout=5000&socketTimeout=10000", host, port, database);
        }
    }

    public static class DiscordConfig {
        public String botToken = "YOUR_BOT_TOKEN_HERE";
        public String requiredGuildId = "123456789012345678";
        public String chatChannelId = "123456789012345678";
        public String chatWebhookUrl = "";
    }

    public static ModConfig getInstance() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    public static ModConfig load() {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE);

        if (Files.exists(configPath)) {
            try {
                String json = Files.readString(configPath);
                instance = GSON.fromJson(json, ModConfig.class);
                return instance;
            } catch (IOException e) {
                throw new RuntimeException("Failed to load config file", e);
            }
        } else {
            instance = new ModConfig();
            instance.save();
            return instance;
        }
    }

    public void save() {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE);
        try {
            Files.writeString(configPath, GSON.toJson(this));
        } catch (IOException e) {
            throw new RuntimeException("Failed to save config file", e);
        }
    }

    public MySqlConfig getMysql() {
        return mysql;
    }

    public DiscordConfig getDiscord() {
        return discord;
    }

    public int getCodeExpirationMinutes() {
        return codeExpirationMinutes;
    }

    public String getLanguage() {
        return language;
    }
}
