/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforgespi.locating;

import org.jetbrains.annotations.Nullable;

/**
 * Attributes of a modfile relating to how it was discovered.
 *
 * @param parent The mod file that logically contains this mod file.
 * @param reader The reader that was used to get a mod-file from jar contents. May be null if the mod file was directly created by a locator.
 */
public record ModFileDiscoveryAttributes(@Nullable IModFile parent,
        @Nullable IModFileReader reader,
        @Nullable IModFileCandidateLocator locator,
        @Nullable IDependencyLocator dependencyLocator) {

    public static final ModFileDiscoveryAttributes DEFAULT = new ModFileDiscoveryAttributes(null, null, null, null);
    public ModFileDiscoveryAttributes withParent(IModFile parent) {
        return new ModFileDiscoveryAttributes(parent, reader, locator, dependencyLocator);
    }

    public ModFileDiscoveryAttributes withReader(IModFileReader reader) {
        return new ModFileDiscoveryAttributes(parent, reader, locator, dependencyLocator);
    }

    public ModFileDiscoveryAttributes withLocator(IModFileCandidateLocator locator) {
        return new ModFileDiscoveryAttributes(parent, reader, locator, dependencyLocator);
    }

    public ModFileDiscoveryAttributes withDependencyLocator(IDependencyLocator dependencyLocator) {
        return new ModFileDiscoveryAttributes(parent, reader, locator, dependencyLocator);
    }

    public ModFileDiscoveryAttributes merge(ModFileDiscoveryAttributes attributes) {
        return new ModFileDiscoveryAttributes(
                attributes.parent != null ? attributes.parent : parent,
                attributes.reader != null ? attributes.reader : reader,
                attributes.locator != null ? attributes.locator : locator,
                attributes.dependencyLocator != null ? attributes.dependencyLocator : dependencyLocator);
    }

    @Override
    public String toString() {
        var result = new StringBuilder();
        result.append("[");
        if (parent != null) {
            result.append("parent: ");
            result.append(parent.getFilePath().getFileName());
        }
        if (locator != null) {
            if (result.length() > 1) {
                result.append(", ");
            }
            result.append("locator: ");
            result.append(locator);
        }
        if (dependencyLocator != null) {
            if (result.length() > 1) {
                result.append(", ");
            }
            result.append("locator: ");
            result.append(dependencyLocator);
        }
        if (reader != null) {
            if (result.length() > 1) {
                result.append(", ");
            }
            result.append("reader: ");
            result.append(reader);
        }
        result.append("]");
        return result.toString();
    }
}
