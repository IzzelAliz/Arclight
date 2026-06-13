/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.event.lifecycle;

import java.util.function.Predicate;
import net.neoforged.fml.DeferredWorkQueue;
import net.neoforged.fml.InterModComms;
import net.neoforged.fml.ModContainer;

/**
 * This is the fourth of four commonly called events during mod core startup.
 *
 * Called after {@link InterModEnqueueEvent}
 *
 * Retrieve {@link InterModComms} {@link InterModComms.IMCMessage} suppliers
 * and process them as you wish with this event.
 *
 * This is a parallel dispatch event.
 *
 * @see #getIMCStream()
 * @see #getIMCStream(Predicate)
 */
public class InterModProcessEvent extends ParallelDispatchEvent {
    public InterModProcessEvent(ModContainer container, DeferredWorkQueue deferredWorkQueue) {
        super(container, deferredWorkQueue);
    }
}
