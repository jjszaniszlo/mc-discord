package com.jjszaniszlo.discordIntegration.discord;

import com.jjszaniszlo.discordIntegration.DiscordIntegration;
import com.jjszaniszlo.discordIntegration.config.ModConfig;
import com.jjszaniszlo.discordIntegration.database.DatabaseManager;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.minecraft.server.MinecraftServer;

import java.util.function.Supplier;

public class DiscordBot {
    private static DiscordBot instance;
    private JDA jda;
    private final ModConfig.DiscordConfig config;

    private DiscordBot(ModConfig.DiscordConfig config) {
        this.config = config;
    }

    public static DiscordBot getInstance() {
        return instance;
    }

    public String getRequiredGuildId() {
        return config.requiredGuildId;
    }

    public static void initialize(ModConfig.DiscordConfig config, DatabaseManager databaseManager, Supplier<MinecraftServer> serverSupplier) {
        instance = new DiscordBot(config);
        instance.start(databaseManager, serverSupplier);
    }

    private void start(DatabaseManager databaseManager, Supplier<MinecraftServer> serverSupplier) {
        Thread botThread = new Thread(() -> {
            try {
                DiscordIntegration.LOGGER.info("Starting Discord bot...");

                jda = JDABuilder.createDefault(config.botToken)
                    .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
                    .setMemberCachePolicy(MemberCachePolicy.ALL)
                    .setChunkingFilter(ChunkingFilter.ALL)
                    .addEventListeners(
                        new LinkCommand(databaseManager, config.requiredGuildId),
                        new ChatRelayListener(config.chatChannelId, serverSupplier),
                        new MemberUpdateListener(config.requiredGuildId, serverSupplier)
                    )
                    .build();

                jda.awaitReady();
                DiscordIntegration.LOGGER.info("Discord bot connected as {} in {} guild(s)",
                    jda.getSelfUser().getName(), jda.getGuilds().size());

                var guild = jda.getGuildById(config.requiredGuildId);
                if (guild != null) {
                    guild.updateCommands()
                        .addCommands(
                            Commands.slash("link", "Link your Minecraft account to Discord")
                                .addOption(OptionType.STRING, "code", "Your verification code from Minecraft", true)
                        )
                        .queue(
                            commands -> DiscordIntegration.LOGGER.info("Discord slash commands registered to guild"),
                            error -> DiscordIntegration.LOGGER.error("Failed to register Discord slash commands", error)
                        );
                } else {
                    DiscordIntegration.LOGGER.error("Could not find guild {} to register commands", config.requiredGuildId);
                }

            } catch (Exception e) {
                DiscordIntegration.LOGGER.error("Failed to start Discord bot", e);
            }
        }, "DiscordBot-Init");

        botThread.setDaemon(true);
        botThread.start();
    }

    public void shutdown() {
        if (jda != null) {
            DiscordIntegration.LOGGER.info("Shutting down Discord bot...");
            jda.shutdown();
            try {
                if (!jda.awaitShutdown(java.time.Duration.ofSeconds(5))) {
                    jda.shutdownNow();
                }
            } catch (InterruptedException e) {
                jda.shutdownNow();
            }
        }
    }

    public JDA getJda() {
        return jda;
    }
}
