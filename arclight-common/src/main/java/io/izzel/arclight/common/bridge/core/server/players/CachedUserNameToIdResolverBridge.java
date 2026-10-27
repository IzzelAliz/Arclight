package io.izzel.arclight.common.bridge.core.server.players;

import net.minecraft.server.players.NameAndId;
import org.jetbrains.annotations.Nullable;

public interface CachedUserNameToIdResolverBridge {

    /** Returns a cached identity and updates its MRU position without expiry checks or lookup. */
    @Nullable
    NameAndId bridge$getIfCached(String name);
}
