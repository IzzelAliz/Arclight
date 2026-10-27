package io.izzel.arclight.common.mixin.core.server.players;

import com.google.common.cache.LoadingCache;
import com.mojang.authlib.GameProfile;
import io.izzel.arclight.common.bridge.core.server.players.ProfileResolverCachedBridge;
import net.minecraft.server.players.ProfileResolver;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Optional;
import java.util.UUID;

@Mixin(ProfileResolver.Cached.class)
public abstract class ProfileResolverCachedMixin implements ProfileResolverCachedBridge {

    @Shadow @Final private LoadingCache<String, Optional<GameProfile>> profileCacheByName;
    @Shadow @Final private LoadingCache<UUID, Optional<GameProfile>> profileCacheById;

    @Override
    @Nullable
    public GameProfile bridge$getIfCached(String name) {
        Optional<GameProfile> cached = this.profileCacheByName.getIfPresent(name);
        return cached == null ? null : cached.orElse(null);
    }

    @Override
    @Nullable
    public GameProfile bridge$getIfCached(UUID id) {
        Optional<GameProfile> cached = this.profileCacheById.getIfPresent(id);
        return cached == null ? null : cached.orElse(null);
    }

    @Override
    public void bridge$add(GameProfile profile) {
        Optional<GameProfile> cached = Optional.of(profile);
        this.profileCacheByName.put(profile.name(), cached);
        this.profileCacheById.put(profile.id(), cached);
    }
}
