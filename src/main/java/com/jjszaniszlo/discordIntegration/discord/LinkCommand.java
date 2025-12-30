package com.jjszaniszlo.discordIntegration.discord;

import com.jjszaniszlo.discordIntegration.DiscordIntegration;
import com.jjszaniszlo.discordIntegration.database.DatabaseManager;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.sql.SQLException;
import java.util.Optional;

public class LinkCommand extends ListenerAdapter {
    private final DatabaseManager databaseManager;
    private final String requiredGuildId;

    public LinkCommand(DatabaseManager databaseManager, String requiredGuildId) {
        this.databaseManager = databaseManager;
        this.requiredGuildId = requiredGuildId;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        DiscordIntegration.LOGGER.info("Received slash command: {}", event.getName());

        if (!event.getName().equals("link")) {
            return;
        }

        event.deferReply(true).queue(
            success -> DiscordIntegration.LOGGER.info("Deferred reply sent"),
            error -> DiscordIntegration.LOGGER.error("Failed to defer reply", error)
        );

        String code = event.getOption("code").getAsString().toUpperCase().trim();
        String discordId = event.getUser().getId();

        var guild = event.getJDA().getGuildById(requiredGuildId);
        if (guild == null) {
            event.getHook().sendMessage(Messages.get(Messages.BOT_CONFIG_ERROR)).queue();
            DiscordIntegration.LOGGER.error("Required guild not found: {}", requiredGuildId);
            return;
        }

        guild.retrieveMemberById(discordId).queue(
            member -> handleLinkRequest(event, code, discordId, member.getUser().getName()),
            error -> {
                event.getHook().sendMessage(Messages.get(Messages.LINK_NOT_IN_GUILD)).queue();
                DiscordIntegration.LOGGER.info("User {} attempted to link but is not in guild", discordId);
            }
        );
    }

    private void handleLinkRequest(SlashCommandInteractionEvent event, String code, String discordId, String discordName) {
        try {
            if (databaseManager.isDiscordIdAlreadyLinked(discordId)) {
                event.getHook().sendMessage(Messages.get(Messages.LINK_ALREADY_LINKED)).queue();
                return;
            }

            Optional<DatabaseManager.PendingVerification> pending = databaseManager.verifyCode(code);
            if (pending.isEmpty()) {
                event.getHook().sendMessage(Messages.get(Messages.LINK_INVALID_CODE)).queue();
                return;
            }

            databaseManager.linkAccount(pending.get().minecraftUuid(), discordId);
            databaseManager.deletePendingVerification(code);

            event.getHook().sendMessage(Messages.get(Messages.LINK_SUCCESS, pending.get().minecraftUsername())).queue();

            DiscordIntegration.LOGGER.info("Linked Discord {} ({}) to Minecraft {} ({})",
                discordName, discordId,
                pending.get().minecraftUsername(), pending.get().minecraftUuid());

        } catch (SQLException e) {
            DiscordIntegration.LOGGER.error("Database error during link for Discord user {}", discordId, e);
            event.getHook().sendMessage(Messages.get(Messages.LINK_DATABASE_ERROR)).queue();
        }
    }
}
