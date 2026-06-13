/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.startup;

/**
 * THis exceptions {@link #getMessage()} will be shown to end-users directly in a fatal error popup.
 */
public class FatalStartupException extends RuntimeException {
    private final StartupArgs startupArgs;

    public FatalStartupException(String message, StartupArgs startupArgs) {
        super(message);
        this.startupArgs = startupArgs;
    }

    public FatalStartupException(String message, StartupArgs startupArgs, Throwable cause) {
        super(message, cause);
        this.startupArgs = startupArgs;
    }

    public StartupArgs getStartupArgs() {
        return startupArgs;
    }
}
