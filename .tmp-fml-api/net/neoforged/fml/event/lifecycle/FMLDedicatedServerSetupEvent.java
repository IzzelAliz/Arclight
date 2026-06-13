/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.event.lifecycle;

import net.neoforged.fml.DeferredWorkQueue;
import net.neoforged.fml.ModContainer;

/**
 * This is the second of four commonly called events during mod core startup.
 *
 * Called before {@link InterModEnqueueEvent}
 * Called after {@link FMLCommonSetupEvent}
 *
 * Called on {@link net.neoforged.api.distmarker.Dist#DEDICATED_SERVER} - the dedicated game server.
 *
 * Alternative to {@link FMLClientSetupEvent}.
 *
 * Do dedicated server specific activities with this event.
 *
 * <em>This event is fired before construction of the dedicated server. Use {@code FMLServerAboutToStartEvent}
 * or {@code FMLServerStartingEvent} to do stuff with the server, in both dedicated
 * and integrated server contexts</em>
 *
 * This is a parallel dispatch event.
 */
public class FMLDedicatedServerSetupEvent extends ParallelDispatchEvent {
    public FMLDedicatedServerSetupEvent(ModContainer container, DeferredWorkQueue deferredWorkQueue) {
        super(container, deferredWorkQueue);
    }
}
