/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.i18n;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class I18nManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(FMLTranslations.class);
    private static final Gson GSON = new Gson();
    private static final String DEFAULT_LOCALE = "en_us";

    static final Map<String, String> DEFAULT_TRANSLATIONS = Collections.unmodifiableMap(loadTranslations(DEFAULT_LOCALE));

    static Map<String, String> currentLocale = DEFAULT_TRANSLATIONS;

    public static void injectTranslations(Map<String, String> translations) {
        currentLocale = Collections.unmodifiableMap(translations);
    }

    public static Map<String, String> loadTranslations(String language) {
        var stream = FMLTranslations.class.getResourceAsStream("/lang/" + language + ".json");
        if (stream != null) {
            try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return GSON.fromJson(reader, new TypeToken<>() {});
            } catch (IOException e) {
                LOGGER.error("Failed to load translations for locale {}", language, e);
                return Map.of();
            }
        }
        return Map.of();
    }
}
