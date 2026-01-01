package com.jjszaniszlo.discordIntegration.event;

import com.jjszaniszlo.discordIntegration.DiscordIntegration;
import com.jjszaniszlo.discordIntegration.config.ModConfig;
import com.jjszaniszlo.discordIntegration.database.DatabaseManager;
import com.jjszaniszlo.discordIntegration.discord.DiscordBot;
import com.jjszaniszlo.discordIntegration.discord.DiscordUtils;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

import java.awt.Color;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Optional;

public class MinecraftChatHandler {

    public static void register() {
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> {
            String messageContent = message.getContent().getString();
            handleChatMessage(sender, messageContent);
            return false;
        });
    }

    private static void handleChatMessage(ServerPlayerEntity player, String messageContent) {
        DatabaseManager db = DatabaseManager.getInstance();
        DiscordBot bot = DiscordBot.getInstance();

        if (db == null) {
            sendDefaultMessage(player, messageContent);
            return;
        }

        try {
            Optional<String> discordId = db.getLinkedDiscordId(player.getUuid());
            if (discordId.isEmpty()) {
                sendDefaultMessage(player, messageContent);
                return;
            }

            if (bot == null || bot.getJda() == null) {
                sendDefaultMessage(player, messageContent);
                return;
            }

            String guildId = bot.getRequiredGuildId();
            var guild = bot.getJda().getGuildById(guildId);
            if (guild == null) {
                sendDefaultMessage(player, messageContent);
                return;
            }

            guild.retrieveMemberById(discordId.get()).queue(
                member -> {
                    broadcastToMinecraft(player, member, messageContent);
                    sendToDiscord(member, messageContent);
                },
                error -> {
                    DiscordIntegration.LOGGER.debug("Could not retrieve member: {}", error.getMessage());
                    sendDefaultMessage(player, messageContent);
                }
            );

        } catch (SQLException e) {
            DiscordIntegration.LOGGER.error("Database error in chat handler", e);
            sendDefaultMessage(player, messageContent);
        }
    }

    private static void sendDefaultMessage(ServerPlayerEntity player, String messageContent) {
        var server = player.getEntityWorld().getServer();
        Text chatMessage = Text.literal("<" + player.getName().getString() + "> " + messageContent);
        server.execute(() -> server.getPlayerManager().broadcast(chatMessage, false));
    }

    private static void broadcastToMinecraft(ServerPlayerEntity sender, Member member, String messageContent) {
        String displayName = member.getEffectiveName();
        Role displayedRole = DiscordUtils.getDisplayedRole(member);
        String roleName = displayedRole != null ? displayedRole.getName() : "Member";
        Color roleColor = member.getColor() != null ? member.getColor() : Color.WHITE;

        int colorValue = (roleColor.getRed() << 16) | (roleColor.getGreen() << 8) | roleColor.getBlue();

        Text chatMessage = Text.literal(roleName + " " + displayName + ": ")
            .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(colorValue)))
            .append(Text.literal(messageContent).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFFFFF))));

        var server = sender.getEntityWorld().getServer();
        server.execute(() -> {
            server.getPlayerManager().broadcast(chatMessage, false);
        });
    }

    private static void sendToDiscord(Member member, String messageContent) {
        String webhookUrl = ModConfig.getInstance().getDiscord().chatWebhookUrl;

        if (webhookUrl == null || webhookUrl.isEmpty()) {
            return;
        }

        String username = member.getEffectiveName();
        String avatarUrl = member.getEffectiveAvatarUrl();

        // Parse @username mentions and convert to Discord mention format
        String processedContent = DiscordUtils.parseMentions(messageContent, member.getGuild());

        Thread.startVirtualThread(() -> {
            try {
                HttpURLConnection connection = (HttpURLConnection) URI.create(webhookUrl).toURL().openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                String json = String.format(
                    "{\"username\":\"%s\",\"avatar_url\":\"%s\",\"content\":\"%s\",\"allowed_mentions\":{\"parse\":[\"users\"]}}",
                    escapeJson(username),
                    avatarUrl,
                    escapeJson(processedContent)
                );

                try (OutputStream os = connection.getOutputStream()) {
                    os.write(json.getBytes(StandardCharsets.UTF_8));
                }

                int responseCode = connection.getResponseCode();
                if (responseCode < 200 || responseCode >= 300) {
                    DiscordIntegration.LOGGER.warn("Webhook returned status: {}", responseCode);
                }

                connection.disconnect();
            } catch (Exception e) {
                DiscordIntegration.LOGGER.error("Failed to send webhook message", e);
            }
        });
    }

    private static String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

}
