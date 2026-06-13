/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.event.lifecycle;

import net.neoforged.fml.DeferredWorkQueue;
import net.neoforged.fml.ModContainer;

/**
 * This is a mostly internal event fired to mod containers that indicates that loading is complete. Mods should not
 * in general override or otherwise attempt to implement this event.
 *
 * @author cpw
 */
public class FMLLoadCompleteEvent extends ParallelDispatchEvent {
    public FMLLoadCompleteEvent(ModContainer container, DeferredWorkQueue deferredWorkQueue) {
        super(container, deferredWorkQueue);
    }
}
