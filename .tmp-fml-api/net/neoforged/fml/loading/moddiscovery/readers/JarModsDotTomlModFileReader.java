/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.loading.moddiscovery.readers;

import com.mojang.logging.LogUtils;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.jar.Manifest;
import net.neoforged.fml.ModLoadingException;
import net.neoforged.fml.ModLoadingIssue;
import net.neoforged.fml.jarcontents.JarContents;
import net.neoforged.fml.loading.LogMarkers;
import net.neoforged.fml.loading.moddiscovery.ModFile;
import net.neoforged.fml.loading.moddiscovery.ModFileParser;
import net.neoforged.fml.loading.moddiscovery.ModJarMetadata;
import net.neoforged.neoforgespi.language.IConfigurable;
import net.neoforged.neoforgespi.language.IModFileInfo;
import net.neoforged.neoforgespi.language.IModInfo;
import net.neoforged.neoforgespi.locating.IModFile;
import net.neoforged.neoforgespi.locating.IModFileReader;
import net.neoforged.neoforgespi.locating.ModFileDiscoveryAttributes;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * Responsible for handling mod files that are explicitly marked as mods or libraries via metadata files.
 */
public class JarModsDotTomlModFileReader implements IModFileReader {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final String MODS_TOML = "META-INF/neoforge.mods.toml";
    public static final String MANIFEST = "META-INF/MANIFEST.MF";

    public static IModFile createModFile(JarContents contents, ModFileDiscoveryAttributes discoveryAttributes) {
        var type = getModType(contents);
        IModFile mod;
        if (contents.containsFile(MODS_TOML)) {
            LOGGER.debug(LogMarkers.SCAN, "Found {} mod of type {}: {}", MODS_TOML, type, contents.getPrimaryPath());
            var mjm = new ModJarMetadata();
            mod = new ModFile(contents, mjm, ModFileParser::modsTomlParser, discoveryAttributes);
            mjm.setModFile(mod);
        } else if (type != null) {
            LOGGER.debug(LogMarkers.SCAN, "Found {} mod of type {}: {}", MANIFEST, type, contents.getPrimaryPath());
            mod = new ModFile(contents, null, JarModsDotTomlModFileReader::manifestParser, type, discoveryAttributes);
        } else {
            return null;
        }

        return mod;
    }

    @Nullable
    private static IModFile.Type getModType(JarContents jar) {
        Manifest jarManifest = jar.getManifest();
        if (jarManifest == null) {
            return null;
        }
        var typeString = jarManifest.getMainAttributes().getValue(ModFile.TYPE);
        try {
            return typeString != null ? IModFile.Type.valueOf(typeString) : null;
        } catch (IllegalArgumentException e) {
            throw new ModLoadingException(ModLoadingIssue.error(
                    "fml.modloadingissue.brokenfile.unknownfmlmodtype", typeString).withAffectedPath(jar.getPrimaryPath()));
        }
    }

    public static IModFileInfo manifestParser(IModFile mod) {
        Function<String, Optional<String>> cfg = name -> Optional.ofNullable(mod.getContents().getManifest().getMainAttributes().getValue(name));
        var license = cfg.apply("LICENSE").orElse("");
        var dummy = new IConfigurable() {
            @Override
            public <T> Optional<T> getConfigElement(String... key) {
                return Optional.empty();
            }

            @Override
            public List<? extends IConfigurable> getConfigList(String... key) {
                return Collections.emptyList();
            }
        };

        return new DefaultModFileInfo(mod, license, dummy);
    }

    @Override
    public @Nullable IModFile read(JarContents jar, ModFileDiscoveryAttributes discoveryAttributes) {
        return createModFile(jar, discoveryAttributes.withReader(this));
    }

    private record DefaultModFileInfo(IModFile mod, String license,
            IConfigurable configurable) implements IModFileInfo, IConfigurable {
        @Override
        public <T> Optional<T> getConfigElement(String... strings) {
            return Optional.empty();
        }

        @Override
        public List<? extends IConfigurable> getConfigList(String... strings) {
            return null;
        }

        @Override
        public List<IModInfo> getMods() {
            return Collections.emptyList();
        }

        @Override
        public List<LanguageSpec> requiredLanguageLoaders() {
            return Collections.emptyList();
        }

        @Override
        public boolean showAsResourcePack() {
            return false;
        }

        @Override
        public boolean showAsDataPack() {
            return false;
        }

        @Override
        public Map<String, Object> getFileProperties() {
            return Collections.emptyMap();
        }

        @Override
        public String getLicense() {
            return license;
        }

        @Override
        public IModFile getFile() {
            return mod;
        }

        @Override
        public IConfigurable getConfig() {
            return configurable;
        }

        // These Should never be called as it's only called from ModJarMetadata.version and we bypass that
        @Override
        public String versionString() {
            return null;
        }

        @Override
        public List<String> usesServices() {
            return null;
        }

        @Override
        public String toString() {
            return "IModFileInfo(" + mod.getFilePath() + ")";
        }
    }

    @Override
    public String toString() {
        return "mod manifest";
    }
}
