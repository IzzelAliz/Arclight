/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.loading.moddiscovery;

import com.mojang.logging.LogUtils;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.neoforged.fml.loading.LogMarkers;
import net.neoforged.fml.loading.StringUtils;
import net.neoforged.neoforgespi.language.IConfigurable;
import net.neoforged.neoforgespi.language.IModFileInfo;
import net.neoforged.neoforgespi.language.IModInfo;
import net.neoforged.neoforgespi.language.MavenVersionAdapter;
import net.neoforged.neoforgespi.locating.InvalidModFileException;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;

public class ModFileInfo implements IModFileInfo, IConfigurable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final IConfigurable config;
    private final ModFile modFile;
    private final URL issueURL;
    private final List<LanguageSpec> languageSpecs;
    private final boolean showAsResourcePack;
    private final boolean showAsDataPack;
    private final List<IModInfo> mods;
    private final Map<String, Object> properties;
    private final String license;
    private final List<String> usesServices;

    @ApiStatus.Internal
    public ModFileInfo(ModFile modFile, IConfigurable config, Consumer<IModFileInfo> configFileConsumer) {
        this.modFile = modFile;
        this.config = config;
        configFileConsumer.accept(this);
        var modLoader = config.<String>getConfigElement("modLoader").orElse(null);
        var modLoaderVersion = config.<String>getConfigElement("loaderVersion")
                .map(MavenVersionAdapter::createFromVersionSpec)
                .orElse(null);
        if (modLoaderVersion != null && modLoader == null) {
            throw new InvalidModFileException("You cannot specify a loaderVersion without specifying a modLoader", this);
        }
        this.languageSpecs = new ArrayList<>(List.of(new LanguageSpec(modLoader, modLoaderVersion)));
        // the remaining properties are optional with sensible defaults
        this.license = config.<String>getConfigElement("license")
                .orElse("");
        // Validate the license is set. Only apply this validation to mods.
        if (this.license.isBlank()) {
            throw new InvalidModFileException("Missing license", this);
        }
        this.showAsResourcePack = config.<Boolean>getConfigElement("showAsResourcePack")
                .orElse(false);
        this.showAsDataPack = config.<Boolean>getConfigElement("showAsDataPack")
                .orElse(false);
        this.usesServices = config.<List<String>>getConfigElement("services")
                .orElse(List.of());
        this.properties = config.<Map<String, Object>>getConfigElement("properties")
                .orElse(Collections.emptyMap());
        this.modFile.setFileProperties(this.properties);
        this.issueURL = config.<String>getConfigElement("issueTrackerURL")
                .map(StringUtils::toURL)
                .orElse(null);
        List<? extends IConfigurable> modConfigs = config.getConfigList("mods");
        if (modConfigs.isEmpty()) {
            throw new InvalidModFileException("Missing mods list", this);
        }
        this.mods = modConfigs.stream()
                .map(mi -> (IModInfo) new ModInfo(this, mi))
                .toList();
        if (LOGGER.isDebugEnabled(LogMarkers.LOADING)) {
            LOGGER.debug(LogMarkers.LOADING, "Found valid mod file {} with {} mods - versions {}",
                    this.modFile.getFileName(),
                    this.mods.stream().map(IModInfo::getModId).collect(Collectors.joining(",", "{", "}")),
                    this.mods.stream().map(IModInfo::getVersion).map(Objects::toString).collect(Collectors.joining(",", "{", "}")));
        }
    }

    public ModFileInfo(ModFile file, IConfigurable config, Consumer<IModFileInfo> configFileConsumer, List<LanguageSpec> languageSpecs) {
        this(file, config, configFileConsumer);
        this.languageSpecs.addAll(languageSpecs);
    }

    @Override
    public List<IModInfo> getMods() {
        return mods;
    }

    public ModFile getFile() {
        return this.modFile;
    }

    @Override
    public List<LanguageSpec> requiredLanguageLoaders() {
        return this.languageSpecs;
    }

    @Override
    public Map<String, Object> getFileProperties() {
        return this.properties;
    }

    @Override
    public boolean showAsResourcePack() {
        return this.showAsResourcePack;
    }

    @Override
    public boolean showAsDataPack() {
        return this.showAsDataPack;
    }

    @Override
    public <T> Optional<T> getConfigElement(String... key) {
        return this.config.getConfigElement(key);
    }

    @Override
    public List<? extends IConfigurable> getConfigList(String... key) {
        return this.config.getConfigList(key);
    }

    @Override
    public String getLicense() {
        return license;
    }

    @Override
    public IConfigurable getConfig() {
        return this;
    }

    public URL getIssueURL() {
        return issueURL;
    }

    public Optional<String> getCodeSigningFingerprint() {
        return this.modFile.getContents().getChecksum();
    }

    @Override
    public String versionString() {
        return getMods().get(0).getVersion().toString();
    }

    @Override
    public List<String> usesServices() {
        return usesServices;
    }

    @Override
    public String toString() {
        return modFile.getId();
    }
}
