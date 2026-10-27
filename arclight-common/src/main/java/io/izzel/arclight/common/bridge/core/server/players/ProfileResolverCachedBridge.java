package io.izzel.arclight.common.bridge.core.server.players;

import com.mojang.authlib.GameProfile;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface ProfileResolverCachedBridge {

    /** Returns the cached full profile without loading, or null for a cache miss or cached absence. */
    @Nullable
    GameProfile bridge$getIfCached(String name);

    /** Returns the cached full profile without loading, or null for a cache miss or cached absence. */
    @Nullable
    GameProfile bridge$getIfCached(UUID id);

    /** Stores the supplied profile, including its properties, under its exact name and UUID without loading. */
    void bridge$add(GameProfile profile);
}
