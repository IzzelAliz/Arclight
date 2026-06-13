/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.loading.moddiscovery;

import com.electronwill.nightconfig.core.UnmodifiableCommentedConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.neoforged.fml.loading.LogMarkers;
import net.neoforged.fml.loading.moddiscovery.readers.JarModsDotTomlModFileReader;
import net.neoforged.neoforgespi.language.IConfigurable;
import net.neoforged.neoforgespi.language.IModFileInfo;
import net.neoforged.neoforgespi.locating.IModFile;
import net.neoforged.neoforgespi.locating.InvalidModFileException;
import net.neoforged.neoforgespi.locating.ModFileInfoParser;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class ModFileParser {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static IModFileInfo readModList(ModFile modFile, ModFileInfoParser parser) {
        return parser.build(modFile);
    }

    public static IModFileInfo modsTomlParser(IModFile imodFile) {
        ModFile modFile = (ModFile) imodFile;
        LOGGER.debug(LogMarkers.LOADING, "Considering mod file candidate {}", modFile.getFilePath());
        var modsjson = modFile.getContents().get(JarModsDotTomlModFileReader.MODS_TOML);
        if (modsjson == null) {
            LOGGER.warn(LogMarkers.LOADING, "Mod file {} is missing {} file", modFile.getFilePath(), JarModsDotTomlModFileReader.MODS_TOML);
            return null;
        }

        UnmodifiableCommentedConfig config;
        try (var reader = modsjson.bufferedReader()) {
            config = TomlFormat.instance().createParser().parse(reader).unmodifiable();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read " + modsjson + " from " + imodFile, e);
        }
        NightConfigWrapper configWrapper = new NightConfigWrapper(config);
        return new ModFileInfo(modFile, configWrapper, configWrapper::setFile);
    }

    /**
     * Represents a potential mixin configuration.
     *
     * @param config          The name of the mixin configuration.
     * @param requiredMods    The mod ids that are required for this mixin configuration to be loaded. If empty, will be loaded regardless.
     * @param behaviorVersion The mixin version whose behavior this configuration requests; if unspecified, the default is provided by FML.
     */
    public record MixinConfig(String config, List<String> requiredMods, @Nullable ArtifactVersion behaviorVersion) {}

    protected static List<MixinConfig> getMixinConfigs(IModFileInfo modFileInfo) {
        try {
            var config = modFileInfo.getConfig();
            var mixinsEntries = config.getConfigList("mixins");

            var potentialMixins = new ArrayList<MixinConfig>();
            for (IConfigurable mixinsEntry : mixinsEntries) {
                var name = mixinsEntry.<String>getConfigElement("config")
                        .orElseThrow(() -> new InvalidModFileException("Missing \"config\" in [[mixins]] entry", modFileInfo));
                var requiredModIds = mixinsEntry.<List<String>>getConfigElement("requiredMods").orElse(List.of());
                var behaviorVersion = mixinsEntry.<String>getConfigElement("behaviorVersion")
                        .map(DefaultArtifactVersion::new)
                        .orElse(null);
                potentialMixins.add(new MixinConfig(name, requiredModIds, behaviorVersion));
            }

            return potentialMixins;
        } catch (Exception exception) {
            LOGGER.error("Failed to load mixin configs from mod file", exception);
            return List.of();
        }
    }

    protected static Optional<List<String>> getAccessTransformers(IModFileInfo modFileInfo) {
        try {
            var config = modFileInfo.getConfig();
            var atEntries = config.getConfigList("accessTransformers");
            if (atEntries.isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(atEntries
                    .stream()
                    .map(entry -> entry
                            .<String>getConfigElement("file")
                            .orElseThrow(
                                    () -> new InvalidModFileException("Missing \"file\" in [[accessTransformers]] entry", modFileInfo)))
                    .toList());
        } catch (Exception exception) {
            LOGGER.error("Failed to load access transformers from mod file", exception);
            return Optional.of(List.of());
        }
    }
}
