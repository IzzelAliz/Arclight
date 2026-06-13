/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.event.lifecycle;

import net.neoforged.fml.DeferredWorkQueue;
import net.neoforged.fml.ModContainer;

/**
 * This is the second of four commonly called events during mod lifecycle startup.
 *
 * Called before {@link InterModEnqueueEvent}
 * Called after {@link FMLCommonSetupEvent}
 *
 * Called on {@link net.neoforged.api.distmarker.Dist#CLIENT} - the game client.
 *
 * Alternative to {@link FMLDedicatedServerSetupEvent}.
 *
 * Do client only setup with this event, such as KeyBindings.
 *
 * This is a parallel dispatch event.
 */
public class FMLClientSetupEvent extends ParallelDispatchEvent {
    public FMLClientSetupEvent(ModContainer container, DeferredWorkQueue deferredWorkQueue) {
        super(container, deferredWorkQueue);
    }
}
