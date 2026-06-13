/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.event.lifecycle;

import net.neoforged.fml.DeferredWorkQueue;
import net.neoforged.fml.InterModComms;
import net.neoforged.fml.ModContainer;

/**
 * This is the third of four commonly called events during mod core startup.
 *
 * Called before {@link InterModProcessEvent}
 * Called after {@link FMLClientSetupEvent} or {@link FMLDedicatedServerSetupEvent}
 *
 *
 * Enqueue {@link InterModComms} messages to other mods with this event.
 *
 * This is a parallel dispatch event.
 */
public class InterModEnqueueEvent extends ParallelDispatchEvent {
    public InterModEnqueueEvent(ModContainer container, DeferredWorkQueue deferredWorkQueue) {
        super(container, deferredWorkQueue);
    }
}
