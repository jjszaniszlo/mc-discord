package com.jjszaniszlo.discordIntegration.discord;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

public final class DiscordUtils {
    private DiscordUtils() {}

    public static Role getDisplayedRole(Member member) {
        for (Role role : member.getRoles()) {
            if (role.isHoisted() || role.getColor() != null) {
                return role;
            }
        }
        return member.getRoles().isEmpty() ? null : member.getRoles().getFirst();
    }
}
