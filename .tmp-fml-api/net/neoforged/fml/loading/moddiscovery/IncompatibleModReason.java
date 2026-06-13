/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.loading.moddiscovery;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Predicate;
import net.neoforged.fml.jarcontents.JarContents;
import net.neoforged.fml.loading.StringUtils;

/**
 * When we find a jar file that no {@link net.neoforged.neoforgespi.locating.IModFileReader} can handle,
 * we try to detect if the mod potentially came from another modding system and warn the user about it
 * not being compatible.
 */
public enum IncompatibleModReason {
    OLDFORGE(filePresent("mcmod.info")),
    MINECRAFT_FORGE(filePresent("META-INF/mods.toml")),
    FABRIC(filePresent("fabric.mod.json")),
    QUILT(filePresent("quilt.mod.json")),
    LITELOADER(filePresent("litemod.json")),
    OPTIFINE(filePresent("optifine/Installer.class")),
    BUKKIT(filePresent("plugin.yml"));

    private final Predicate<JarContents> ident;

    IncompatibleModReason(Predicate<JarContents> identifier) {
        this.ident = identifier;
    }

    public String getReason() {
        return "fml.modloadingissue.brokenfile." + StringUtils.toLowerCase(name());
    }

    public static Optional<IncompatibleModReason> detect(JarContents jar) {
        return Arrays.stream(values())
                .filter(i -> i.ident.test(jar))
                .findAny();
    }

    private static Predicate<JarContents> filePresent(String filename) {
        return jarContents -> jarContents.containsFile(filename);
    }
}
