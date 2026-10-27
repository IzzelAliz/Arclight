package io.izzel.arclight.common.mod.util;

import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.nio.channels.ClosedChannelException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Pending cookie requests owned by one native connection, shared across its listeners and wrappers.
 */
public final class ConnectionCookieRequests {

    private final Map<Identifier, Deque<CompletableFuture<byte[]>>> pending = new HashMap<>();
    private boolean closed;

    /**
     * Registers and sends a request under the same lock so FIFO order matches send order.
     * A cancelled future remains queued until its own response arrives.
     *
     * @param send sends exactly this request, synchronously enqueueing it on the connection
     * @return the response future, completed exceptionally if sending throws or the connection is closed
     */
    public CompletableFuture<byte[]> request(Identifier key, Runnable send) {
        Objects.requireNonNull(key, "Cookie key cannot be null");
        Objects.requireNonNull(send, "Cookie sender cannot be null");
        CompletableFuture<byte[]> future = new CompletableFuture<>();
        Throwable failure = null;
        synchronized (this.pending) {
            if (this.closed) {
                failure = new ClosedChannelException();
            } else {
                Deque<CompletableFuture<byte[]>> queue = this.pending.computeIfAbsent(key, ignored -> new ArrayDeque<>());
                queue.addLast(future);
                try {
                    send.run();
                } catch (RuntimeException | Error exception) {
                    queue.removeFirstOccurrence(future);
                    if (queue.isEmpty()) {
                        // Sending may have reentered and replaced this key's queue.
                        this.pending.remove(key, queue);
                    }
                    failure = exception;
                }
            }
        }
        if (failure != null) {
            future.completeExceptionally(failure);
        }
        return future;
    }

    /**
     * Consumes only the oldest request for this key, including cancelled requests.
     * Missing cookies are represented by a null payload; unknown keys are not consumed.
     */
    public boolean handleResponse(Identifier key, byte @Nullable [] payload) {
        CompletableFuture<byte[]> future;
        synchronized (this.pending) {
            Deque<CompletableFuture<byte[]>> queue = this.pending.get(key);
            if (queue == null) {
                return false;
            }
            future = queue.removeFirst();
            if (queue.isEmpty()) {
                this.pending.remove(key);
            }
        }
        // Remove before completing: callbacks can immediately request the same key again.
        future.complete(payload);
        return true;
    }

    /**
     * Terminates this connection's requests, not an individual listener's ownership.
     * Drain before completing so callbacks cannot register another request on the closed connection.
     */
    public void close() {
        List<CompletableFuture<byte[]>> futures = new ArrayList<>();
        synchronized (this.pending) {
            if (this.closed) {
                return;
            }
            this.closed = true;
            for (Deque<CompletableFuture<byte[]>> queue : this.pending.values()) {
                futures.addAll(queue);
                queue.clear();
            }
            this.pending.clear();
        }
        for (CompletableFuture<byte[]> future : futures) {
            future.completeExceptionally(new ClosedChannelException());
        }
    }

    public boolean isAwaitingCookies() {
        synchronized (this.pending) {
            return !this.pending.isEmpty();
        }
    }
}
