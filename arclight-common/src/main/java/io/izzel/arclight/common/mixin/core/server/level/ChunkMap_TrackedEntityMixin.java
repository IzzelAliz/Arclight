package io.izzel.arclight.common.mixin.core.server.level;

import io.izzel.arclight.common.bridge.core.world.TrackedEntityBridge;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(ChunkMap.TrackedEntity.class)
public abstract class ChunkMap_TrackedEntityMixin {

    // @formatter:off
    @Shadow @Final ServerEntity serverEntity;
    @Shadow @Final public Set<ServerPlayer> seenBy;
    // @formatter:on

    @Inject(method = "<init>", at = @At("RETURN"))
    private void arclight$setTrackedPlayers(ChunkMap outer, Entity entity, int range, int updateFrequency, boolean sendVelocityUpdates, CallbackInfo ci) {
        ((TrackedEntityBridge) this.serverEntity).bridge$setTrackedPlayers(this.seenBy);
    }
}
