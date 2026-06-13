/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.loading.moddiscovery.locators;

import com.electronwill.nightconfig.core.Config;
import java.util.List;
import net.neoforged.fml.loading.moddiscovery.ModFile;
import net.neoforged.fml.loading.moddiscovery.ModFileInfo;
import net.neoforged.fml.loading.moddiscovery.NightConfigWrapper;
import net.neoforged.neoforgespi.language.IModFileInfo;
import net.neoforged.neoforgespi.locating.IModFile;

final class MinecraftModInfo {
    private final String minecraftVersion;

    public MinecraftModInfo(String minecraftVersion) {
        this.minecraftVersion = minecraftVersion;
    }

    public IModFileInfo buildMinecraftModInfo(IModFile iModFile) {
        ModFile modFile = (ModFile) iModFile;

        // We haven't changed this in years, and I can't be asked right now to special case this one file in the path.
        var conf = Config.inMemory();
        conf.set("modLoader", "minecraft");
        conf.set("loaderVersion", "1");
        conf.set("license", "Mojang Studios, All Rights Reserved");
        var mods = Config.inMemory();
        mods.set("modId", "minecraft");
        mods.set("version", minecraftVersion);
        mods.set("displayName", "Minecraft");
        mods.set("description", "Minecraft");
        conf.set("mods", List.of(mods));

        NightConfigWrapper configWrapper = new NightConfigWrapper(conf);
        return new ModFileInfo(modFile, configWrapper, configWrapper::setFile, List.of());
    }
}
