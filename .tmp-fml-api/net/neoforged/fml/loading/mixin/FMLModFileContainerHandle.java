/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.loading.mixin;

import java.util.Collection;
import java.util.List;
import net.neoforged.neoforgespi.locating.IModFile;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;

/**
 * We use this to expose mod files that are not mods to Mixin, purely to allow it scanning for
 * manifest attributes so game libraries/plugins can also contribute Mixins. One example for
 * this is mixinextras.
 */
final class FMLModFileContainerHandle implements IContainerHandle {
    private final IModFile modFile;

    public FMLModFileContainerHandle(IModFile modFile) {
        this.modFile = modFile;
    }

    @Override
    public String getAttribute(String name) {
        return modFile.getContents().getManifest().getMainAttributes().getValue(name);
    }

    @Override
    public Collection<IContainerHandle> getNestedContainers() {
        return List.of();
    }

    @Override
    public String getId() {
        return modFile.getId();
    }

    @Override
    public String getDescription() {
        return modFile.toString();
    }

    @Override
    public String toString() {
        return modFile.toString();
    }
}
