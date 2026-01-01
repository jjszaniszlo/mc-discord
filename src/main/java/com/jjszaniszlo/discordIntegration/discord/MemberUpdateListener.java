package com.jjszaniszlo.discordIntegration.discord;

import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.minecraft.server.MinecraftServer;

import java.util.function.Supplier;

public class MemberUpdateListener extends ListenerAdapter {
    private final Supplier<MinecraftServer> serverSupplier;
    private final String requiredGuildId;

    public MemberUpdateListener(String requiredGuildId, Supplier<MinecraftServer> serverSupplier) {
        this.requiredGuildId = requiredGuildId;
        this.serverSupplier = serverSupplier;
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        if (!event.getGuild().getId().equals(requiredGuildId)) {
            return;
        }

        MinecraftServer server = serverSupplier.get();
        if (server != null) {
            server.execute(() ->
                MentionSuggestions.addMemberToAllPlayers(server, event.getMember())
            );
        }
    }

    @Override
    public void onGuildMemberRemove(GuildMemberRemoveEvent event) {
        if (!event.getGuild().getId().equals(requiredGuildId)) {
            return;
        }

        MinecraftServer server = serverSupplier.get();
        if (server != null && event.getMember() != null) {
            server.execute(() ->
                MentionSuggestions.removeMemberFromAllPlayers(server, event.getMember())
            );
        }
    }

    @Override
    public void onGuildMemberUpdateNickname(GuildMemberUpdateNicknameEvent event) {
        if (!event.getGuild().getId().equals(requiredGuildId)) {
            return;
        }

        MinecraftServer server = serverSupplier.get();
        if (server != null) {
            server.execute(() ->
                MentionSuggestions.sendToAllPlayers(server)
            );
        }
    }
}
