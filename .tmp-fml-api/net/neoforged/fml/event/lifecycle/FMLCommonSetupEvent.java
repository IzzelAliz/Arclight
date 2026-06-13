/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.event.lifecycle;

import java.util.function.Consumer;
import net.neoforged.fml.DeferredWorkQueue;
import net.neoforged.fml.ModContainer;

/**
 * This is the first of four commonly called events during mod initialization.
 *
 * Called after {@link net.neoforged.registries.RegisterEvent} events have been fired and before
 * {@link FMLClientSetupEvent} or {@link FMLDedicatedServerSetupEvent} during mod startup.
 *
 * Either register your listener using {@link net.neoforged.fml.javafmlmod.AutomaticEventSubscriber} and
 * {@link net.neoforged.bus.api.SubscribeEvent} or
 * {@link net.neoforged.bus.api.IEventBus#addListener(Consumer)} in your constructor.
 *
 * Most non-specific mod setup will be performed here. Note that this is a parallel dispatched event - you cannot
 * interact with game state in this event.
 *
 * @see DeferredWorkQueue to enqueue work to run on the main game thread after this event has
 *      completed dispatch
 */
public class FMLCommonSetupEvent extends ParallelDispatchEvent {
    public FMLCommonSetupEvent(ModContainer container, DeferredWorkQueue deferredWorkQueue) {
        super(container, deferredWorkQueue);
    }
}
