package com.jjszaniszlo.discordIntegration.discord;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.jjszaniszlo.discordIntegration.DiscordIntegration;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class Messages {
    private static final Gson GSON = new Gson();
    private static Map<String, String> messages = new HashMap<>();

    public static final String LINK_SUCCESS = "link.success";
    public static final String LINK_ALREADY_LINKED = "link.already_linked";
    public static final String LINK_INVALID_CODE = "link.invalid_code";
    public static final String LINK_NOT_IN_GUILD = "link.not_in_guild";
    public static final String LINK_DATABASE_ERROR = "link.database_error";
    public static final String BOT_CONFIG_ERROR = "bot.config_error";

    public static void load(String language) {
        Path configPath = FabricLoader.getInstance().getConfigDir()
            .resolve("discord-integration")
            .resolve("messages_" + language + ".json");

        if (Files.exists(configPath)) {
            try {
                String json = Files.readString(configPath);
                messages = GSON.fromJson(json, new TypeToken<Map<String, String>>(){}.getType());
                DiscordIntegration.LOGGER.info("Loaded Discord messages from {}", configPath);
                return;
            } catch (IOException e) {
                DiscordIntegration.LOGGER.warn("Failed to load messages from {}, falling back to built-in", configPath);
            }
        }

        String resourcePath = "/assets/discord-integration/messages/" + language + ".json";
        try (InputStream is = Messages.class.getResourceAsStream(resourcePath)) {
            if (is != null) {
                messages = GSON.fromJson(new InputStreamReader(is, StandardCharsets.UTF_8),
                    new TypeToken<Map<String, String>>(){}.getType());
                DiscordIntegration.LOGGER.info("Loaded built-in Discord messages for language: {}", language);
            } else {
                if (!language.equals("en")) {
                    DiscordIntegration.LOGGER.warn("Messages for language '{}' not found, falling back to English", language);
                    load("en");
                } else {
                    loadDefaults();
                }
            }
        } catch (IOException e) {
            DiscordIntegration.LOGGER.error("Failed to load messages", e);
            loadDefaults();
        }
    }

    private static void loadDefaults() {
        messages.put(LINK_SUCCESS, "Successfully linked! Your Discord account is now linked to Minecraft account **%s**. You can now join the server!");
        messages.put(LINK_ALREADY_LINKED, "Your Discord account is already linked to a Minecraft account!");
        messages.put(LINK_INVALID_CODE, "Invalid or expired verification code. Please get a new code by joining the Minecraft server.");
        messages.put(LINK_NOT_IN_GUILD, "You must be a member of this server to link your account!");
        messages.put(LINK_DATABASE_ERROR, "A database error occurred. Please try again later.");
        messages.put(BOT_CONFIG_ERROR, "Bot configuration error. Please contact an administrator.");
    }

    public static String get(String key) {
        return messages.getOrDefault(key, key);
    }

    public static String get(String key, Object... args) {
        String message = messages.getOrDefault(key, key);
        try {
            return String.format(message, args);
        } catch (Exception e) {
            return message;
        }
    }
}
