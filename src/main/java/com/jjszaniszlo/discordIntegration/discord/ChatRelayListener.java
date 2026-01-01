package com.jjszaniszlo.discordIntegration.discord;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

import java.awt.Color;
import java.util.function.Supplier;

public class ChatRelayListener extends ListenerAdapter {
    private final String chatChannelId;
    private final Supplier<MinecraftServer> serverSupplier;

    public ChatRelayListener(String chatChannelId, Supplier<MinecraftServer> serverSupplier) {
        this.chatChannelId = chatChannelId;
        this.serverSupplier = serverSupplier;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) {
            return;
        }

        if (!event.getChannel().getId().equals(chatChannelId)) {
            return;
        }

        MinecraftServer server = serverSupplier.get();
        if (server == null) {
            return;
        }

        Member member = event.getMember();
        if (member == null) {
            return;
        }

        String displayName = member.getEffectiveName();
        String message = event.getMessage().getContentDisplay();

        if (message.isEmpty()) {
            return;
        }

        Role displayedRole = DiscordUtils.getDisplayedRole(member);
        String roleName = displayedRole != null ? displayedRole.getName() : "Member";
        Color roleColor = member.getColor() != null ? member.getColor() : Color.WHITE;

        int colorValue = (roleColor.getRed() << 16) | (roleColor.getGreen() << 8) | roleColor.getBlue();

        Text chatMessage = Text.literal("D ")
            .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x5865F2)).withBold(true))
            .append(Text.literal(roleName + " " + displayName + ": ")
                .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(colorValue)).withBold(false)))
            .append(Text.literal(message).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFFFFF)).withBold(false)));

        server.execute(() -> {
            server.getPlayerManager().broadcast(chatMessage, false);
        });
    }

}
