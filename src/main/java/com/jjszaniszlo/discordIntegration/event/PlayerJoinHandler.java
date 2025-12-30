package com.jjszaniszlo.discordIntegration.event;

import com.jjszaniszlo.discordIntegration.DiscordIntegration;
import com.jjszaniszlo.discordIntegration.database.DatabaseManager;
import com.jjszaniszlo.discordIntegration.i18n.ServerMessages;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PlayerJoinHandler {

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            UUID uuid = player.getUuid();
            String username = player.getName().getString();
            String lang = player.getClientOptions() != null ? player.getClientOptions().language() : "en_us";

            CompletableFuture.runAsync(() -> {
                try {
                    DatabaseManager db = DatabaseManager.getInstance();
                    if (db == null) {
                        DiscordIntegration.LOGGER.error("Database not initialized, kicking player {}", username);
                        server.execute(() -> player.networkHandler.disconnect(
                            Text.literal(ServerMessages.get(lang, ServerMessages.CONFIG_ERROR))));
                        return;
                    }

                    Optional<String> discordId = db.getLinkedDiscordId(uuid);

                    if (discordId.isEmpty()) {
                        String code = db.createPendingVerification(uuid, username);
                        String command = "/link " + code;

                        Text kickMessage = Text.literal(ServerMessages.get(lang, ServerMessages.NOT_LINKED_TITLE))
                            .append(Text.literal("\n\n"))
                            .append(Text.literal(ServerMessages.get(lang, ServerMessages.NOT_LINKED_STEP1)))
                            .append(Text.literal("\n"))
                            .append(Text.literal(ServerMessages.get(lang, ServerMessages.NOT_LINKED_STEP2)))
                            .append(Text.literal(command)
                                .setStyle(Style.EMPTY
                                    .withColor(Formatting.AQUA)
                                    .withBold(true)
                                    .withUnderline(true)
                                    .withClickEvent(new ClickEvent.CopyToClipboard(command))));

                        server.execute(() -> {
                            player.networkHandler.disconnect(kickMessage);
                            DiscordIntegration.LOGGER.info("Player {} kicked - not linked. Code: {}", username, code);
                        });
                    } else {
                        DiscordIntegration.LOGGER.info("Player {} joined - linked to Discord ID: {}", username, discordId.get());
                    }
                } catch (Exception e) {
                    DiscordIntegration.LOGGER.error("Error during player verification for {}", username, e);
                    server.execute(() -> player.networkHandler.disconnect(
                        Text.literal(ServerMessages.get(lang, ServerMessages.SERVER_ERROR))));
                }
            });
        });
    }
}
