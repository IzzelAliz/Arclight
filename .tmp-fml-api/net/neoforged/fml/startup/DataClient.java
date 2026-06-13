/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.startup;

import net.neoforged.api.distmarker.Dist;

public class DataClient extends Entrypoint {
    private DataClient() {}

    public static void main(String[] args) {
        try (var startupResult = startup(args, true, Dist.CLIENT, false)) {
            var main = createMainMethodCallable(startupResult, "net.minecraft.client.data.Main");
            main.invokeExact(startupResult.loader().getProgramArgs().getArguments());
        } catch (Throwable t) {
            FatalErrorReporting.reportFatalErrorOnConsole(t);
            System.exit(1);
        }
    }
}
