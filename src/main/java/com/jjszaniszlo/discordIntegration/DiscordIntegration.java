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

        try {
            DatabaseManager.initialize(config.getMysql());
            LOGGER.info("Database connection established");
            DiscordBot.initialize(config.getDiscord(), DatabaseManager.getInstance(), serverRef::get);
        } catch (Exception e) {
            LOGGER.error("Failed to initialize: {}", e.getMessage());
        }

        PlayerJoinHandler.register();
        MinecraftChatHandler.register();

        ServerLifecycleEvents.SERVER_STARTED.register(server -> serverRef.set(server));

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
}
