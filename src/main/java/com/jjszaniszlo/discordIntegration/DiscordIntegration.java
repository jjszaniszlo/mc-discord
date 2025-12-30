package com.jjszaniszlo.discordIntegration;

import com.jjszaniszlo.discordIntegration.config.ModConfig;
import com.jjszaniszlo.discordIntegration.database.DatabaseManager;
import com.jjszaniszlo.discordIntegration.discord.DiscordBot;
import com.jjszaniszlo.discordIntegration.discord.Messages;
import com.jjszaniszlo.discordIntegration.event.MinecraftChatHandler;
import com.jjszaniszlo.discordIntegration.event.PlayerJoinHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicReference;

public class DiscordIntegration implements ModInitializer {
    public static final String MOD_ID = "discord-integration";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final AtomicReference<MinecraftServer> serverRef = new AtomicReference<>();

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Discord Integration mod...");

        ModConfig config = ModConfig.getInstance();
        LOGGER.info("Configuration loaded");

        Messages.load(config.getLanguage());

        boolean databaseReady = false;
        try {
            DatabaseManager.initialize(config.getMysql());
            LOGGER.info("Database connection established");
            databaseReady = true;
        } catch (Exception e) {
            LOGGER.error("Failed to initialize database: {}", e.getMessage());
        }

        if (databaseReady && !config.getDiscord().botToken.equals("YOUR_BOT_TOKEN_HERE")) {
            DiscordBot.initialize(config.getDiscord(), DatabaseManager.getInstance(), serverRef::get);
        } else if (!databaseReady) {
            LOGGER.warn("Discord bot not started - database required");
        } else {
            LOGGER.warn("Discord bot token not configured");
        }

        PlayerJoinHandler.register();
        MinecraftChatHandler.register();

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            serverRef.set(server);
            LOGGER.info("Server reference captured for chat relay");
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            serverRef.set(null);
            if (DiscordBot.getInstance() != null) {
                DiscordBot.getInstance().shutdown();
            }
            if (DatabaseManager.getInstance() != null) {
                DatabaseManager.getInstance().close();
            }
        });

        LOGGER.info("Discord Integration initialized");
    }

    public static MinecraftServer getServer() {
        return serverRef.get();
    }
}
