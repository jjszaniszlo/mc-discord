package com.jjszaniszlo.discordIntegration.discord;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.minecraft.network.packet.s2c.play.ChatSuggestionsS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;

public final class MentionSuggestions {
    private MentionSuggestions() {}

    public static void sendToPlayer(ServerPlayerEntity player) {
        List<String> suggestions = getDiscordUsernameSuggestions();
        if (!suggestions.isEmpty()) {
            player.networkHandler.sendPacket(new ChatSuggestionsS2CPacket(
                ChatSuggestionsS2CPacket.Action.ADD,
                suggestions
            ));
        }
    }

    public static void sendToAllPlayers(MinecraftServer server) {
        if (server == null) {
            return;
        }

        List<String> suggestions = getDiscordUsernameSuggestions();
        if (suggestions.isEmpty()) {
            return;
        }

        ChatSuggestionsS2CPacket packet = new ChatSuggestionsS2CPacket(
            ChatSuggestionsS2CPacket.Action.ADD,
            suggestions
        );

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.networkHandler.sendPacket(packet);
        }
    }

    public static void addMemberToAllPlayers(MinecraftServer server, Member member) {
        if (server == null || member.getUser().isBot()) {
            return;
        }

        List<String> suggestions = new ArrayList<>();
        String username = member.getUser().getName();
        suggestions.add("@" + username);

        String displayName = member.getEffectiveName();
        String normalizedDisplayName = DiscordUtils.normalizeName(displayName);
        if (normalizedDisplayName.length() >= 2 && !normalizedDisplayName.equalsIgnoreCase(username)) {
            suggestions.add("@" + normalizedDisplayName);
        }

        ChatSuggestionsS2CPacket packet = new ChatSuggestionsS2CPacket(
            ChatSuggestionsS2CPacket.Action.ADD,
            suggestions
        );

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.networkHandler.sendPacket(packet);
        }
    }

    public static void removeMemberFromAllPlayers(MinecraftServer server, Member member) {
        if (server == null || member.getUser().isBot()) {
            return;
        }

        List<String> suggestions = new ArrayList<>();
        String username = member.getUser().getName();
        suggestions.add("@" + username);

        String displayName = member.getEffectiveName();
        String normalizedDisplayName = DiscordUtils.normalizeName(displayName);
        if (normalizedDisplayName.length() >= 2 && !normalizedDisplayName.equalsIgnoreCase(username)) {
            suggestions.add("@" + normalizedDisplayName);
        }

        ChatSuggestionsS2CPacket packet = new ChatSuggestionsS2CPacket(
            ChatSuggestionsS2CPacket.Action.REMOVE,
            suggestions
        );

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.networkHandler.sendPacket(packet);
        }
    }

    private static List<String> getDiscordUsernameSuggestions() {
        List<String> suggestions = new ArrayList<>();

        DiscordBot bot = DiscordBot.getInstance();
        if (bot == null || bot.getJda() == null) {
            return suggestions;
        }

        Guild guild = bot.getJda().getGuildById(bot.getRequiredGuildId());
        if (guild == null) {
            return suggestions;
        }

        for (Member member : guild.getMembers()) {
            if (member.getUser().isBot()) {
                continue;
            }

            String username = member.getUser().getName();
            String displayName = member.getEffectiveName();
            String normalizedDisplayName = DiscordUtils.normalizeName(displayName);

            suggestions.add("@" + username);

            if (normalizedDisplayName.length() >= 2 && !normalizedDisplayName.equalsIgnoreCase(username)) {
                suggestions.add("@" + normalizedDisplayName);
            }
        }

        return suggestions;
    }
}
