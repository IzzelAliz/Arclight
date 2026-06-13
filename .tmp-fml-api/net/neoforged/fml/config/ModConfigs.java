/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.config;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.neoforged.fml.ModContainer;

/**
 * This class provides access to all mod configs known to FML.
 * It can be used by mods that want to process all configs.
 * Configs are registered via {@link ModContainer#registerConfig(ModConfig.Type, IConfigSpec)}.
 */
public final class ModConfigs {
    public static List<ModConfig> getModConfigs(String modId) {
        return Collections.unmodifiableList(ConfigTracker.INSTANCE.configsByMod.getOrDefault(modId, List.of()));
    }

    public static List<String> getConfigFileNames(String modId, ModConfig.Type type) {
        var config = ConfigTracker.INSTANCE.configsByMod.getOrDefault(modId, List.of());
        synchronized (config) { // Synchronized list: requires explicit synchronization for stream
            return config.stream()
                    .filter(c -> c.getType() == type)
                    .map(ModConfig::getFileName)
                    .toList();
        }
    }

    public static Set<ModConfig> getConfigSet(ModConfig.Type type) {
        return Collections.unmodifiableSet(ConfigTracker.INSTANCE.configSets.get(type));
    }

    public static Map<String, ModConfig> getFileMap() {
        return Collections.unmodifiableMap(ConfigTracker.INSTANCE.fileMap);
    }
}
