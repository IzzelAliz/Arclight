package io.izzel.arclight.common.mod.util;

import net.kyori.adventure.resource.ResourcePackCallback;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Resource-pack callbacks owned by one native connection across listener replacements.
 */
public final class ConnectionResourcePackCallbacks {

    private final Map<UUID, ResourcePackCallback> pending = new HashMap<>();
    private boolean closed;

    /**
     * Registers before sending, replacing any callback for the same pack ID.
     * Rejecting a closed connection throws so callers do not continue to send as if registered.
     */
    public void register(UUID id, ResourcePackCallback callback) {
        Objects.requireNonNull(id, "Resource pack ID cannot be null");
        Objects.requireNonNull(callback, "Resource pack callback cannot be null");
        synchronized (this.pending) {
            if (this.closed) {
                throw new IllegalStateException("Connection is closed");
            }
            this.pending.put(id, callback);
        }
    }

    /**
     * Detaches terminal callbacks before the caller invokes plugin code outside this lock.
     */
    @Nullable
    public ResourcePackCallback response(UUID id, boolean terminal) {
        Objects.requireNonNull(id, "Resource pack ID cannot be null");
        synchronized (this.pending) {
            return terminal ? this.pending.remove(id) : this.pending.get(id);
        }
    }

    /**
     * Releases references only; transport termination is not a resource-pack response.
     */
    public void close() {
        synchronized (this.pending) {
            this.closed = true;
            this.pending.clear();
        }
    }
}
