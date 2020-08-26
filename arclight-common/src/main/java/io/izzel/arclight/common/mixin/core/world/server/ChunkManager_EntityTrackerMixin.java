package io.izzel.arclight.common.mixin.core.world.server;

import io.izzel.arclight.common.bridge.world.TrackedEntityBridge;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.world.TrackedEntity;
import net.minecraft.world.server.ChunkManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(ChunkManager.EntityTracker.class)
public abstract class ChunkManager_EntityTrackerMixin {

    // @formatter:off
    @Shadow @Final private TrackedEntity entry;
    @Shadow @Final public Set<ServerPlayerEntity> trackingPlayers;
    // @formatter:on

    @Inject(method = "<init>", at = @At("RETURN"))
    private void arclight$setTrackedPlayers(ChunkManager outer, Entity entity, int p_i50468_3_, int updateFrequency, boolean sendVelocityUpdates, CallbackInfo ci) {
        ((TrackedEntityBridge) this.entry).bridge$setTrackedPlayers(this.trackingPlayers);
    }
}
