/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforgespi.locating;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.neoforged.fml.jarcontents.JarContents;
import net.neoforged.fml.jarmoduleinfo.JarModuleInfo;
import net.neoforged.fml.loading.moddiscovery.ModFile;
import net.neoforged.neoforgespi.language.IModFileInfo;
import net.neoforged.neoforgespi.language.IModInfo;
import net.neoforged.neoforgespi.language.ModFileScanData;
import org.jetbrains.annotations.ApiStatus;

/**
 * Represents a single "mod" file in the runtime.
 * <p>
 * Although these are known as "Mod"-Files, they do not always represent mods.
 * However, they should be more treated as an extension or modification of minecraft.
 * And as such can contain any number of things, like language loaders, dependencies of other mods
 * or code which does not interact with minecraft at all and is just a utility for any of the other mod
 * files.
 */
@ApiStatus.NonExtendable
public interface IModFile {
    /**
     * Builds a new mod file instance depending on the current runtime.
     *
     * @param contents The secure jar to load the mod file from.
     * @param parser   The parser which is responsible for parsing the metadata of the file itself.
     * @return The mod file.
     */
    static IModFile create(JarContents contents, ModFileInfoParser parser) throws InvalidModFileException {
        return new ModFile(contents, parser, ModFileDiscoveryAttributes.DEFAULT);
    }

    /**
     * Builds a new mod file instance depending on the current runtime.
     *
     * @param contents The secure jar to load the mod file from.
     * @param parser   The parser which is responsible for parsing the metadata of the file itself.
     * @return The mod file.
     */
    static IModFile create(JarContents contents, JarModuleInfo metadata, ModFileInfoParser parser) throws InvalidModFileException {
        return new ModFile(contents, metadata, parser, ModFileDiscoveryAttributes.DEFAULT);
    }

    /**
     * Builds a new mod file instance depending on the current runtime.
     *
     * @param contents   The secure jar to load the mod file from.
     * @param parser     The parser which is responsible for parsing the metadata of the file itself.
     * @param attributes Additional attributes of the modfile.
     * @return The mod file.
     */
    static IModFile create(JarContents contents, ModFileInfoParser parser, ModFileDiscoveryAttributes attributes) throws InvalidModFileException {
        return new ModFile(contents, parser, attributes);
    }

    /**
     * Builds a new mod file instance depending on the current runtime.
     *
     * @param contents   The secure jar to load the mod file from.
     * @param metadata   Information about the jar contents.
     * @param parser     The parser which is responsible for parsing the metadata of the file itself.
     * @param type       the type of the mod
     * @param attributes Additional attributes of the modfile.
     * @return The mod file.
     */
    static IModFile create(JarContents contents, JarModuleInfo metadata, ModFileInfoParser parser, IModFile.Type type, ModFileDiscoveryAttributes attributes) throws InvalidModFileException {
        return new ModFile(contents, metadata, parser, type, attributes);
    }

    /**
     * Builds a new mod file instance depending on the current runtime.
     *
     * @param contents   The secure jar to load the mod file from.
     * @param parser     The parser which is responsible for parsing the metadata of the file itself.
     * @param type       the type of the mod
     * @param attributes Additional attributes of the modfile.
     * @return The mod file.
     */
    static IModFile create(JarContents contents, ModFileInfoParser parser, IModFile.Type type, ModFileDiscoveryAttributes attributes) throws InvalidModFileException {
        return new ModFile(contents, null, parser, type, attributes);
    }

    /**
     * A unique ID identifying this mod file.
     *
     * <p>For mod files containing mods this will correspond with the mod id of the first mod contained in this file.
     * <p>For non-mod jar files, an approach to generating a unique id is using the same algorithm used by Java
     * to determine a Java module name for a given Jar file, but this is not guaranteed.
     */
    String getId();

    /**
     * {@return the contents of the mod file, which allow direct access to files in the mods jar file}
     */
    JarContents getContents();

    /**
     * The mod files specific string data substitution map.
     * The map returned here is used to interpolate values in the metadata of the included mods.
     * Examples of where this is used in FML: While parsing the mods.toml file, keys like:
     * ${file.xxxxx} are replaced with the values of this map, with keys xxxxx.
     *
     * @return The string substitution map used during metadata load.
     */
    Supplier<Map<String, Object>> getSubstitutionMap();

    /**
     * The type of the mod jar.
     * This primarily defines how and where this mod file is loaded into the module system.
     *
     * @return The type of the mod file.
     */
    Type getType();

    /**
     * The path to the underlying mod file.
     *
     * @return The path to the mod file.
     */
    Path getFilePath();

    /**
     * Returns a list of all mods located inside this jar.
     * <p>
     * If this method returns any entries then {@link #getType()} has to return {@link Type#MOD},
     * else this mod file will not be loaded in the proper module layer in 1.17 and above.
     *
     * @return The mods in this mod file.
     */
    List<IModInfo> getModInfos();

    /**
     * The metadata scan result of all the classes located inside this file.
     *
     * @return The metadata scan result.
     */
    ModFileScanData getScanResult();

    /**
     * The raw file name of this file.
     *
     * @return The raw file name.
     */
    String getFileName();

    /**
     * Get attributes about how this mod file was discovered.
     */
    ModFileDiscoveryAttributes getDiscoveryAttributes();

    /**
     * The metadata info related to this particular file.
     *
     * @return The info for this file.
     */
    IModFileInfo getModFileInfo();

    /**
     * The type of file.
     * Determines into which module layer the data is loaded and how metadata is loaded and processed.
     */
    enum Type {
        /**
         * A mod file holds mod code and loads in the game module layer.
         */
        MOD,
        /**
         * A library can provide lang providers in the plugin module layer,
         * and/or provides code that is not mod- or Minecraft- related.
         */
        LIBRARY,
        /**
         * A game library can reference MC code and loads in the game module layer.
         */
        GAMELIBRARY
    }
}
