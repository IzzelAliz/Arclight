/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.event.lifecycle;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.neoforged.fml.DeferredWorkQueue;
import net.neoforged.fml.ModContainer;

public abstract class ParallelDispatchEvent extends ModLifecycleEvent {
    private final DeferredWorkQueue workQueue;

    public ParallelDispatchEvent(ModContainer container, DeferredWorkQueue workQueue) {
        super(container);
        this.workQueue = workQueue;
    }

    public CompletableFuture<Void> enqueueWork(Runnable work) {
        return workQueue.enqueueWork(getContainer(), work);
    }

    public <T> CompletableFuture<T> enqueueWork(Supplier<T> work) {
        return workQueue.enqueueWork(getContainer(), work);
    }
}
