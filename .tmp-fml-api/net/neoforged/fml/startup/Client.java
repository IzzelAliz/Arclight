/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.startup;

import net.neoforged.api.distmarker.Dist;

/**
 * The entrypoint for starting a modded Minecraft client.
 */
public class Client extends Entrypoint {
    private Client() {}

    public static void main(String[] args) {
        try (var startupResult = startup(args, false, Dist.CLIENT, true)) {
            var main = createMainMethodCallable(startupResult, "net.minecraft.client.main.Main");
            main.invokeExact(startupResult.loader().getProgramArgs().getArguments());
        } catch (Throwable t) {
            FatalErrorReporting.reportFatalError(t);
            System.exit(1);
        }
    }
}
