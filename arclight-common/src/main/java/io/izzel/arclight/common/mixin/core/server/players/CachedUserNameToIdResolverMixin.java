package io.izzel.arclight.common.mixin.core.server.players;

import io.izzel.arclight.common.bridge.core.server.players.CachedUserNameToIdResolverBridge;
import net.minecraft.server.players.CachedUserNameToIdResolver;
import net.minecraft.server.players.NameAndId;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Mixin(CachedUserNameToIdResolver.class)
public abstract class CachedUserNameToIdResolverMixin implements CachedUserNameToIdResolverBridge {

    @Shadow @Final private Map<String, CachedUserNameToIdResolver.GameProfileInfo> profilesByName;
    @Shadow @Final private AtomicLong operationCount;

    @Override
    @Nullable
    public NameAndId bridge$getIfCached(String name) {
        var entry = this.profilesByName.get(name.toLowerCase(Locale.ROOT));
        if (entry == null) {
            return null;
        }
        entry.setLastAccess(this.operationCount.incrementAndGet());
        return entry.nameAndId();
    }
}
