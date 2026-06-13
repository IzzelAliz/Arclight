/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.mclanguageprovider;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.loading.BuiltInLanguageLoader;
import net.neoforged.neoforgespi.language.IModInfo;
import net.neoforged.neoforgespi.language.ModFileScanData;

public class MinecraftModLanguageProvider extends BuiltInLanguageLoader {
    @Override
    public String name() {
        return "minecraft";
    }

    @Override
    public ModContainer loadMod(IModInfo info, ModFileScanData modFileScanResults, ModuleLayer layer) {
        return new MinecraftModContainer(info);
    }
}
