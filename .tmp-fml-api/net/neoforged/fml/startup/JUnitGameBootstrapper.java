/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.startup;

import net.neoforged.fml.loading.FMLLoader;

/**
 * Obtained via service-loader and executed when FML is bootstrapped in a unit testing context.
 * This is implemented by NeoForge to bootstrap the game and run necessary mod loading tasks when
 * mods want a mod-loading environment in their JUnit tests.
 */
public interface JUnitGameBootstrapper {
    void bootstrap(FMLLoader loader);
}
