package com.jjszaniszlo.discordIntegration.discord;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

import java.text.Normalizer;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DiscordUtils {
    private DiscordUtils() {}

    private static final Pattern MENTION_PATTERN = Pattern.compile("@(\\w+)");

    public static String normalizeName(String name) {
        String ascii = toAscii(name);
        return ascii.replace(" ", "_").replaceAll("[^\\w]", "");
    }

    private static String toAscii(String input) {
        StringBuilder result = new StringBuilder(input.length());

        for (int i = 0; i < input.length(); i++) {
            int codePoint = input.codePointAt(i);

            if (Character.isSupplementaryCodePoint(codePoint)) {
                i++;
            }

            char ascii = convertToAscii(codePoint);
            if (ascii != 0) {
                result.append(ascii);
            } else if (codePoint < 128) {
                result.appendCodePoint(codePoint);
            } else {
                String normalized = Normalizer.normalize(
                    String.valueOf(Character.toChars(codePoint)),
                    Normalizer.Form.NFKD
                );
                for (char c : normalized.toCharArray()) {
                    if (c < 128 && !Character.isISOControl(c)) {
                        result.append(c);
                    }
                }
            }
        }

        return result.toString();
    }

    private static char convertToAscii(int codePoint) {
        // Mathematical Bold (U+1D400-1D433)
        if (codePoint >= 0x1D400 && codePoint <= 0x1D419) return (char) ('A' + (codePoint - 0x1D400));
        if (codePoint >= 0x1D41A && codePoint <= 0x1D433) return (char) ('a' + (codePoint - 0x1D41A));

        // Mathematical Italic (U+1D434-1D467)
        if (codePoint >= 0x1D434 && codePoint <= 0x1D44D) return (char) ('A' + (codePoint - 0x1D434));
        if (codePoint >= 0x1D44E && codePoint <= 0x1D467) return (char) ('a' + (codePoint - 0x1D44E));

        // Mathematical Bold Italic (U+1D468-1D49B)
        if (codePoint >= 0x1D468 && codePoint <= 0x1D481) return (char) ('A' + (codePoint - 0x1D468));
        if (codePoint >= 0x1D482 && codePoint <= 0x1D49B) return (char) ('a' + (codePoint - 0x1D482));

        // Mathematical Script (U+1D49C-1D4CF) - includes 𝒻𝓇𝑒𝒶𝓀𝓎
        if (codePoint >= 0x1D49C && codePoint <= 0x1D4B5) return (char) ('A' + (codePoint - 0x1D49C));
        if (codePoint >= 0x1D4B6 && codePoint <= 0x1D4CF) return (char) ('a' + (codePoint - 0x1D4B6));

        // Mathematical Bold Script (U+1D4D0-1D503)
        if (codePoint >= 0x1D4D0 && codePoint <= 0x1D4E9) return (char) ('A' + (codePoint - 0x1D4D0));
        if (codePoint >= 0x1D4EA && codePoint <= 0x1D503) return (char) ('a' + (codePoint - 0x1D4EA));

        // Mathematical Fraktur (U+1D504-1D537)
        if (codePoint >= 0x1D504 && codePoint <= 0x1D51C) return (char) ('A' + (codePoint - 0x1D504));
        if (codePoint >= 0x1D51E && codePoint <= 0x1D537) return (char) ('a' + (codePoint - 0x1D51E));

        // Mathematical Double-Struck (U+1D538-1D56B)
        if (codePoint >= 0x1D538 && codePoint <= 0x1D551) return (char) ('A' + (codePoint - 0x1D538));
        if (codePoint >= 0x1D552 && codePoint <= 0x1D56B) return (char) ('a' + (codePoint - 0x1D552));

        // Mathematical Bold Fraktur (U+1D56C-1D59F)
        if (codePoint >= 0x1D56C && codePoint <= 0x1D585) return (char) ('A' + (codePoint - 0x1D56C));
        if (codePoint >= 0x1D586 && codePoint <= 0x1D59F) return (char) ('a' + (codePoint - 0x1D586));

        // Mathematical Sans-Serif (U+1D5A0-1D5D3)
        if (codePoint >= 0x1D5A0 && codePoint <= 0x1D5B9) return (char) ('A' + (codePoint - 0x1D5A0));
        if (codePoint >= 0x1D5BA && codePoint <= 0x1D5D3) return (char) ('a' + (codePoint - 0x1D5BA));

        // Mathematical Sans-Serif Bold (U+1D5D4-1D607)
        if (codePoint >= 0x1D5D4 && codePoint <= 0x1D5ED) return (char) ('A' + (codePoint - 0x1D5D4));
        if (codePoint >= 0x1D5EE && codePoint <= 0x1D607) return (char) ('a' + (codePoint - 0x1D5EE));

        // Mathematical Sans-Serif Italic (U+1D608-1D63B)
        if (codePoint >= 0x1D608 && codePoint <= 0x1D621) return (char) ('A' + (codePoint - 0x1D608));
        if (codePoint >= 0x1D622 && codePoint <= 0x1D63B) return (char) ('a' + (codePoint - 0x1D622));

        // Mathematical Sans-Serif Bold Italic (U+1D63C-1D66F)
        if (codePoint >= 0x1D63C && codePoint <= 0x1D655) return (char) ('A' + (codePoint - 0x1D63C));
        if (codePoint >= 0x1D656 && codePoint <= 0x1D66F) return (char) ('a' + (codePoint - 0x1D656));

        // Mathematical Monospace (U+1D670-1D6A3)
        if (codePoint >= 0x1D670 && codePoint <= 0x1D689) return (char) ('A' + (codePoint - 0x1D670));
        if (codePoint >= 0x1D68A && codePoint <= 0x1D6A3) return (char) ('a' + (codePoint - 0x1D68A));

        return 0;
    }

    public static Role getDisplayedRole(Member member) {
        for (Role role : member.getRoles()) {
            if (role.isHoisted() || role.getColor() != null) {
                return role;
            }
        }
        return member.getRoles().isEmpty() ? null : member.getRoles().getFirst();
    }

    public static String parseMentions(String message, Guild guild) {
        if (guild == null || message == null) {
            return message;
        }

        List<Member> members = guild.getMembers();
        Matcher matcher = MENTION_PATTERN.matcher(message);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String username = matcher.group(1);
            Member foundMember = findMemberByName(members, username);

            if (foundMember != null) {
                matcher.appendReplacement(result, "<@" + foundMember.getId() + ">");
            }
        }
        matcher.appendTail(result);

        return result.toString();
    }

    private static Member findMemberByName(List<Member> members, String name) {
        String lowerName = name.toLowerCase();

        for (Member member : members) {
            if (member.getUser().getName().equalsIgnoreCase(name)) {
                return member;
            }
        }

        for (Member member : members) {
            String displayName = member.getEffectiveName();
            if (displayName.equalsIgnoreCase(name) ||
                normalizeName(displayName).equalsIgnoreCase(name)) {
                return member;
            }
        }

        for (Member member : members) {
            if (member.getUser().getName().toLowerCase().startsWith(lowerName)) {
                return member;
            }
        }

        for (Member member : members) {
            String displayName = member.getEffectiveName();
            if (displayName.toLowerCase().startsWith(lowerName) ||
                normalizeName(displayName).toLowerCase().startsWith(lowerName)) {
                return member;
            }
        }

        return null;
    }
}
