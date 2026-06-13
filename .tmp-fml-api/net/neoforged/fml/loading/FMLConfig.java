/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.loading;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.ConfigSpec;
import com.electronwill.nightconfig.core.InMemoryFormat;
import com.electronwill.nightconfig.core.io.ParsingMode;
import com.electronwill.nightconfig.core.io.WritingMode;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.electronwill.nightconfig.toml.TomlParser;
import com.electronwill.nightconfig.toml.TomlWriter;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jetbrains.annotations.Unmodifiable;
import org.jetbrains.annotations.UnmodifiableView;
import org.slf4j.Logger;

public class FMLConfig {
    public enum ConfigValue {
        DISABLE_CONFIG_WATCHER("disableConfigWatcher", Boolean.FALSE, "Disables File Watcher. Used to automatically update config if its file has been modified."),
        EARLY_WINDOW_CONTROL("earlyWindowControl", Boolean.TRUE, "Shows an early loading screen for mod loading which improves the user experience with early feedback about mod loading."),
        MAX_THREADS("maxThreads", -1, "Max threads for early initialization parallelism,  -1 is based on processor count", FMLConfig::maxThreads),
        VERSION_CHECK("versionCheck", Boolean.TRUE, "Enable NeoForge global version checking"),
        DEBUG_OPENGL("debugOpenGl", Boolean.FALSE, "Enable synchronous OpenGL debug output and object labeling"),
        DEFAULT_CONFIG_PATH("defaultConfigPath", "defaultconfigs", "Default config path for servers"),
        DISABLE_OPTIMIZED_DFU("disableOptimizedDFU", Boolean.TRUE, "Disables Optimized DFU client-side - already disabled on servers"),
        EARLY_WINDOW_PROVIDER("earlyWindowProvider", "fmlearlywindow", "Early window provider"),
        EARLY_WINDOW_WIDTH("earlyWindowWidth", 854, "Early window width"),
        EARLY_WINDOW_HEIGHT("earlyWindowHeight", 480, "Early window height"),
        EARLY_WINDOW_MAXIMIZED("earlyWindowMaximized", Boolean.FALSE, "Early window starts maximized"),
        EARLY_LOADING_SCREEN_THEME("earlyLoadingScreenTheme", "", "Force a given theme-id to be used for the early loading screen");

        private final String entry;
        private final Object defaultValue;
        private final String comment;
        private final Class<?> valueType;
        private final Function<Object, Object> entryFunction;

        ConfigValue(String entry, Object defaultValue, String comment) {
            this(entry, defaultValue, comment, Function.identity());
        }

        ConfigValue(String entry, Object defaultValue, String comment, Function<Object, Object> entryFunction) {
            this.entry = entry;
            this.defaultValue = defaultValue;
            this.comment = comment;
            this.valueType = defaultValue.getClass();
            this.entryFunction = entryFunction;
        }

        void buildConfigEntry(ConfigSpec spec, CommentedConfig commentedConfig) {
            if (this.defaultValue instanceof List<?> list) {
                spec.defineList(this.entry, list, e -> e instanceof String);
            } else {
                spec.define(this.entry, this.defaultValue);
            }
            commentedConfig.add(this.entry, this.defaultValue);
            commentedConfig.setComment(this.entry, this.comment);
        }

        @SuppressWarnings("unchecked")
        private <T> T getConfigValue(CommentedConfig config) {
            return (T) this.entryFunction.apply(config != null ? config.get(this.entry) : this.defaultValue);
        }

        private <T> void setConfigValue(CommentedConfig configData, T value) {
            configData.set(this.entry, value);
        }
    }

    private static Object maxThreads(Object value) {
        int val = (Integer) value;
        if (val <= 0) return Runtime.getRuntime().availableProcessors();
        else return val;
    }

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final FMLConfig INSTANCE = new FMLConfig();
    private static Map<String, List<DependencyOverride>> dependencyOverrides = Map.of();
    private static final ConfigSpec configSpec = new ConfigSpec(
            // Make sure the values are written in the same order as the enum.
            InMemoryFormat.withUniversalSupport().createConfig(LinkedHashMap::new));
    private static final CommentedConfig configComments = CommentedConfig.inMemory();

    static {
        for (ConfigValue cv : ConfigValue.values()) {
            cv.buildConfigEntry(configSpec, configComments);
        }

        // Make sure that we don't end up "correcting" the config and removing dependency overrides
        // We accept any objects (parsing and validation is done when the config is loaded)
        configSpec.define("dependencyOverrides", () -> null, object -> true);
        configComments.set("dependencyOverrides", configComments.createSubConfig());
        configComments.setComment("dependencyOverrides", """
                Define dependency overrides below
                Dependency overrides can be used to forcibly remove a dependency constraint from a mod or to force a mod to load AFTER another mod
                Using dependency overrides can cause issues. Use at your own risk.
                Example dependency override for the mod with the id 'targetMod': dependency constraints (incompatibility clauses or restrictive version ranges) against mod 'dep1' are removed, and the mod will now load after the mod 'dep2'
                dependencyOverrides.targetMod = ["-dep1", "+dep2"]""");
    }

    private CommentedConfig configData;

    private void loadFrom(Path configFile) {
        this.configData = CommentedConfig.of(LinkedHashMap::new, TomlFormat.instance());

        if (Files.exists(configFile)) {
            try (var configStream = Files.newInputStream(configFile)) {
                new TomlParser().parse(configStream, configData, ParsingMode.REPLACE);
            } catch (IOException e) {
                throw new RuntimeException("Failed to read FML config", e);
            }

            if (!configSpec.isCorrect(this.configData)) {
                LOGGER.warn(LogMarkers.CORE, "Configuration file {} is not correct. Correcting", configFile);
                configSpec.correct(this.configData, (action, path, incorrectValue, correctedValue) -> LOGGER.warn(LogMarkers.CORE, "Incorrect key {} was corrected from {} to {}", path, incorrectValue, correctedValue));
            }
        } else {
            // This populates the config with the default values.
            configSpec.correct(this.configData);

            // Since dependency overrides have an empty validator, they need to be added manually.
            // (Correct doesn't correct an absent value since it's valid).
            this.configData.set("dependencyOverrides", this.configData.createSubConfig());
        }

        this.configData.putAllComments(configComments);
        saveConfig(configFile);
    }

    private void saveConfig(Path configFile) {
        new TomlWriter().write(this.configData, configFile, WritingMode.REPLACE);
    }

    public static void load() {
        Path configFile = FMLPaths.FMLCONFIG.get();
        INSTANCE.loadFrom(configFile);
        if (LOGGER.isTraceEnabled(LogMarkers.CORE)) {
            LOGGER.trace(LogMarkers.CORE, "Loaded FML config from {}", FMLPaths.FMLCONFIG.get());
            for (ConfigValue cv : ConfigValue.values()) {
                LOGGER.trace(LogMarkers.CORE, "FMLConfig {} is {}", cv.entry, cv.getConfigValue(INSTANCE.configData));
            }
        }
        FMLPaths.getOrCreateGameRelativePath(Paths.get(FMLConfig.getConfigValue(ConfigValue.DEFAULT_CONFIG_PATH)));

        // load dependency overrides
        Map<String, List<DependencyOverride>> dependencyOverrides = new HashMap<>();
        var overridesObject = INSTANCE.configData.get("dependencyOverrides");
        if (overridesObject != null) {
            if (!(overridesObject instanceof Config cfg)) {
                LOGGER.error("Invalid dependency overrides declaration in config. Expected object but found {}", overridesObject);
                return;
            }

            cfg.valueMap().forEach((modId, object) -> {
                // We accept both dependencyOverrides.target = "-dep" and dependencyOverrides.target = ["-dep"]
                var asList = object instanceof List<?> ls ? ls : List.of(object);
                var overrides = dependencyOverrides.computeIfAbsent(modId, k -> new ArrayList<>());
                for (Object o : asList) {
                    var str = (String) o;
                    var start = str.charAt(0);
                    if (start != '+' && start != '-') {
                        LOGGER.error("Found invalid dependency override for mod '{}'. Expected +/- in override '{}'. Did you forget to specify the override type?", modId, str);
                    } else {
                        var removal = start == '-';
                        var depMod = str.substring(1);
                        overrides.add(new DependencyOverride(depMod, removal));
                    }
                }
            });
        }

        if (!dependencyOverrides.isEmpty()) {
            LOGGER.warn("*".repeat(30) + " Found dependency overrides " + "*".repeat(30));
            dependencyOverrides.forEach((modId, ov) -> LOGGER.warn("Dependency overrides for mod '{}': {}", modId, ov.stream().map(DependencyOverride::getMessage).collect(Collectors.joining(", "))));
            LOGGER.warn("*".repeat(88));
        }

        // Make the overrides immutable
        dependencyOverrides.replaceAll((id, list) -> List.copyOf(list));
        FMLConfig.dependencyOverrides = Collections.unmodifiableMap(dependencyOverrides);
    }

    public static String getConfigValue(ConfigValue v) {
        return v.getConfigValue(INSTANCE.configData);
    }

    public static boolean getBoolConfigValue(ConfigValue v) {
        return v.getConfigValue(INSTANCE.configData);
    }

    public static int getIntConfigValue(ConfigValue v) {
        return v.getConfigValue(INSTANCE.configData);
    }

    public static <A> List<A> getListConfigValue(ConfigValue v) {
        return v.getConfigValue(INSTANCE.configData);
    }

    public static <T> void updateConfig(ConfigValue v, T value) {
        if (INSTANCE.configData != null) {
            v.setConfigValue(INSTANCE.configData, value);
            INSTANCE.saveConfig(FMLPaths.FMLCONFIG.get());
        }
    }

    public static String defaultConfigPath() {
        return getConfigValue(ConfigValue.DEFAULT_CONFIG_PATH);
    }

    @Unmodifiable
    public static List<DependencyOverride> getOverrides(String modId) {
        var ov = dependencyOverrides.get(modId);
        if (ov == null) return List.of();
        return ov;
    }

    @UnmodifiableView
    public static Map<String, List<DependencyOverride>> getDependencyOverrides() {
        return Collections.unmodifiableMap(dependencyOverrides);
    }

    public record DependencyOverride(String modId, boolean remove) {
        public String getMessage() {
            return (remove ? "softening dependency constraints against" : "adding explicit AFTER ordering against") + " '" + modId + "'";
        }
    }
}
