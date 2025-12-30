package com.jjszaniszlo.discordIntegration.i18n;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.jjszaniszlo.discordIntegration.DiscordIntegration;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ServerMessages {
    private static final Gson GSON = new Gson();
    private static final Map<String, Map<String, String>> languageMessages = new HashMap<>();
    private static final String DEFAULT_LANGUAGE = "en_us";

    public static final String NOT_LINKED_TITLE = "not_linked.title";
    public static final String NOT_LINKED_STEP1 = "not_linked.step1";
    public static final String NOT_LINKED_STEP2 = "not_linked.step2";
    public static final String SERVER_ERROR = "server_error";
    public static final String CONFIG_ERROR = "config_error";

    static {
        loadLanguage("en_us");
        loadLanguage("es_es");
        loadLanguage("es_mx");
        loadLanguage("es_ar");
        loadLanguage("es_cl");
        loadLanguage("es_ec");
        loadLanguage("es_uy");
        loadLanguage("es_ve");
    }

    private static void loadLanguage(String langCode) {
        String resourcePath = "/assets/discord-integration/lang/server/" + langCode + ".json";
        try (InputStream is = ServerMessages.class.getResourceAsStream(resourcePath)) {
            if (is != null) {
                Map<String, String> messages = GSON.fromJson(
                    new InputStreamReader(is, StandardCharsets.UTF_8),
                    new TypeToken<Map<String, String>>(){}.getType()
                );
                languageMessages.put(langCode, messages);
                DiscordIntegration.LOGGER.debug("Loaded server messages for language: {}", langCode);
            }
        } catch (IOException e) {
            DiscordIntegration.LOGGER.debug("No server messages found for language: {}", langCode);
        }
    }

    public static String get(String langCode, String key) {
        String normalizedLang = langCode.toLowerCase();

        Map<String, String> messages = languageMessages.get(normalizedLang);

        if (messages == null && normalizedLang.contains("_")) {
            String baseLang = normalizedLang.split("_")[0];
            for (String lang : languageMessages.keySet()) {
                if (lang.startsWith(baseLang + "_")) {
                    messages = languageMessages.get(lang);
                    break;
                }
            }
        }

        if (messages == null) {
            messages = languageMessages.get(DEFAULT_LANGUAGE);
        }

        if (messages == null) {
            return key;
        }

        return messages.getOrDefault(key, languageMessages.get(DEFAULT_LANGUAGE).getOrDefault(key, key));
    }

    public static String get(String langCode, String key, Object... args) {
        String message = get(langCode, key);
        try {
            return String.format(message, args);
        } catch (Exception e) {
            return message;
        }
    }
}
