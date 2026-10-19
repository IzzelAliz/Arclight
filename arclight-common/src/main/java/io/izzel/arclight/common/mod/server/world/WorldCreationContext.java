package io.izzel.arclight.common.mod.server.world;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.WorldGenSettings;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

/** Carries world settings while Bukkit constructs a ServerLevel. */
public final class WorldCreationContext {
    private static WorldGenSettings pending;
    private static ServerLevel constructing;
    private static final Map<ServerLevel, WorldGenSettings> LEVELS = Collections.synchronizedMap(new IdentityHashMap<>());

    private WorldCreationContext() {}

    public static void begin(WorldGenSettings settings) {
        pending = settings;
        constructing = null;
    }

    public static WorldGenSettings current() {
        return pending;
    }

    public static WorldGenSettings attachIfPresent(ServerLevel level) {
        if (pending != null) {
            constructing = level;
            LEVELS.put(level, pending);
        }
        return pending;
    }

    public static void attach(ServerLevel level) {
        attachIfPresent(level);
    }

    public static boolean ownsPending(ServerLevel level) {
        return constructing == level;
    }

    public static void complete(ServerLevel level) {
        constructing = null;
    }

    public static WorldGenSettings lookup(ServerLevel level) {
        return LEVELS.get(level);
    }

    public static WorldGenSettings require(ServerLevel level) {
        return java.util.Objects.requireNonNull(lookup(level), "Missing world generation settings");
    }

    public static void discard(ServerLevel level) {
        LEVELS.remove(level);
        if (constructing == level) constructing = null;
    }

    public static void clearPending() {
        if (constructing != null) LEVELS.remove(constructing);
        constructing = null;
        pending = null;
    }
}
