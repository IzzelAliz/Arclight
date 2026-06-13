/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.loading.mixin;

import java.net.URL;
import net.neoforged.fml.loading.FMLLoader;
import org.spongepowered.asm.service.IClassProvider;

/**
 * Class provider for use under ModLauncher
 */
class FMLClassProvider implements IClassProvider {
    FMLClassProvider() {}

    @Override
    @Deprecated
    public URL[] getClassPath() {
        return new URL[0];
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        return Class.forName(name, true, FMLLoader.getCurrent().getCurrentClassLoader());
    }

    @Override
    public Class<?> findClass(String name, boolean initialize) throws ClassNotFoundException {
        return Class.forName(name, initialize, FMLLoader.getCurrent().getCurrentClassLoader());
    }

    @Override
    public Class<?> findAgentClass(String name, boolean initialize) throws ClassNotFoundException {
        return Class.forName(name, initialize, getClass().getClassLoader());
    }
}
