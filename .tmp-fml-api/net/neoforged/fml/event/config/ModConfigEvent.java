/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.event.config;

import net.neoforged.bus.api.Event;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.IModBusEvent;

public class ModConfigEvent extends Event implements IModBusEvent {
    private final ModConfig config;

    ModConfigEvent(ModConfig config) {
        this.config = config;
    }

    public ModConfig getConfig() {
        return config;
    }

    /**
     * Fired during mod and server loading, depending on {@link ModConfig.Type} of config file.
     * Any Config objects associated with this will be valid and can be queried directly.
     */
    public static class Loading extends ModConfigEvent {
        public Loading(ModConfig config) {
            super(config);
        }
    }

    /**
     * Fired when the configuration is changed. This can be caused by a change to the config
     * from a UI or from editing the file itself. IMPORTANT: this can fire at any time
     * and may not even be on the server or client threads. Ensure you properly synchronize
     * any resultant changes.
     */
    public static class Reloading extends ModConfigEvent {
        public Reloading(ModConfig config) {
            super(config);
        }
    }

    /**
     * Fired when a config is unloaded - that is, when a server (integrated or dedicated) shuts down or when a player disconnects from a remote server.
     * The config file will be saved after this event has fired.
     */
    public static class Unloading extends ModConfigEvent {
        public Unloading(ModConfig config) {
            super(config);
        }
    }
}
