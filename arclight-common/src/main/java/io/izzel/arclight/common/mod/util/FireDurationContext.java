package io.izzel.arclight.common.mod.util;

import net.minecraft.world.entity.Entity;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Map;

/** Bounded per-applyEffectsFromBlocks duration hand-off for BaseFireBlock only. */
public final class FireDurationContext {

    private static final ThreadLocal<Deque<Map<Entity, Float>>> SCOPES = new ThreadLocal<>();

    private FireDurationContext() {
    }

    public static void pushScope() {
        Deque<Map<Entity, Float>> scopes = SCOPES.get();
        if (scopes == null) {
            scopes = new ArrayDeque<>();
            SCOPES.set(scopes);
        }
        scopes.push(new IdentityHashMap<>());
    }

    public static void popScope() {
        Deque<Map<Entity, Float>> scopes = SCOPES.get();
        scopes.pop();
        if (scopes.isEmpty()) {
            SCOPES.remove();
        }
    }

    public static void set(Entity entity, float duration) {
        Deque<Map<Entity, Float>> scopes = SCOPES.get();
        if (scopes != null && !scopes.isEmpty()) {
            scopes.peek().put(entity, duration);
        }
    }

    public static float consume(Entity entity, float fallback) {
        Deque<Map<Entity, Float>> scopes = SCOPES.get();
        if (scopes == null || scopes.isEmpty()) {
            return fallback;
        }
        Float duration = scopes.peek().remove(entity);
        return duration == null ? fallback : duration;
    }
}
