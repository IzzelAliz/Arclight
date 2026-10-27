package io.izzel.arclight.common.bridge.core.server;

import java.util.concurrent.CompletableFuture;

/** Completion notification for the native command path, not plugin reload callers. */
public interface CommandResourceReloadBridge {

    CompletableFuture<Void> bridge$notifyCommandResourceReload(CompletableFuture<Void> reload);
}
