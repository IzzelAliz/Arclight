/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.event.lifecycle;

import net.neoforged.fml.DeferredWorkQueue;
import net.neoforged.fml.ModContainer;

public class FMLConstructModEvent extends ParallelDispatchEvent {
    public FMLConstructModEvent(ModContainer container, DeferredWorkQueue deferredWorkQueue) {
        super(container, deferredWorkQueue);
    }
}
