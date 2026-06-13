/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.loading;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import net.neoforged.fml.FMLVersion;
import net.neoforged.neoforgespi.language.IModLanguageLoader;

public abstract class BuiltInLanguageLoader implements IModLanguageLoader {
    @Override
    public String version() {
        Path lpPath;
        try {
            lpPath = Paths.get(this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException("Huh?", e);
        }
        return JarVersionLookupHandler.getVersion(this.getClass()).orElse(Files.isDirectory(lpPath) ? FMLVersion.getVersion() : null);
    }
}
