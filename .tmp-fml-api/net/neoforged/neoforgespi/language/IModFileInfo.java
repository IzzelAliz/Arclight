/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforgespi.language;

import java.util.List;
import java.util.Map;
import net.neoforged.neoforgespi.locating.IModFile;
import org.apache.maven.artifact.versioning.VersionRange;
import org.jetbrains.annotations.Nullable;

public interface IModFileInfo {
    List<IModInfo> getMods();

    record LanguageSpec(@Nullable String languageName, @Nullable VersionRange acceptedVersions) {}

    List<LanguageSpec> requiredLanguageLoaders();

    boolean showAsResourcePack();

    boolean showAsDataPack();

    Map<String, Object> getFileProperties();

    String getLicense();

    String versionString();

    List<String> usesServices();

    IModFile getFile();

    IConfigurable getConfig();
}
