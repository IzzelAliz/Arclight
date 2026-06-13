/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.startup;

import net.neoforged.api.distmarker.Dist;

/**
 * The entrypoint for starting a modded Minecraft server.
 */
public class Server extends Entrypoint {
    private Server() {}

    public static void main(String[] args) {
        try (var startupResult = startup(args, true, Dist.DEDICATED_SERVER, true)) {
            var main = createMainMethodCallable(startupResult, "net.minecraft.server.Main");
            main.invokeExact(startupResult.loader().getProgramArgs().getArguments());

            var serverThread = findThread("Server thread");
            if (serverThread == null) {
                throw new FatalStartupException("Couldn't find Minecraft server thread. Startup likely failed.", startupResult.startupArgs());
            }

            serverThread.join();
        } catch (Throwable t) {
            FatalErrorReporting.reportFatalErrorOnConsole(t);
            System.exit(1);
        }
    }
}
