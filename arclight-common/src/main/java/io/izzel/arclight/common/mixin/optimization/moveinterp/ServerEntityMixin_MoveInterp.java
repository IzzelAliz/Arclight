package io.izzel.arclight.common.mixin.optimization.moveinterp;

import io.izzel.arclight.common.bridge.core.world.ServerEntityBridge;
import io.izzel.arclight.common.mod.util.optimization.moveinterp.MoveInterpolatorService;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(ServerEntity.class)
public abstract class ServerEntityMixin_MoveInterp implements ServerEntityBridge {

    // @formatter:off
    @Shadow @Final @Mutable private Consumer<Packet<?>> broadcast;
    @Shadow @Final private Entity entity;
    // @formatter:on

    @Unique private boolean positionSynched;
    @Unique private boolean motionSynched;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void arclight$filterPacket(ServerLevel level, Entity entity, int interval, boolean trackDelta, Consumer<Packet<?>> broadcast, CallbackInfo ci) {
        if (!(entity instanceof ServerPlayer)) {
            this.broadcast = packet -> {
                if (packet instanceof ClientboundMoveEntityPacket) {
                    return;
                }
                if (packet instanceof ClientboundSetEntityMotionPacket) {
                    motionSynched = true;
                    return;
                }
                if (packet instanceof ClientboundTeleportEntityPacket) {
                    positionSynched = true;
                    return;
                }
                broadcast.accept(packet);
            };
        }
    }

    //@Inject(method = "sendChanges", at = @At("HEAD"))
    //private void arclight$sendParticle(CallbackInfo ci) {
    //    if (!(this.entity instanceof ServerPlayer)) {
    //        var position = this.entity.position();
    //        ((WorldBridge) this.entity.level).bridge$getWorld()
    //            .spawnParticle(Particle.SMALL_FLAME, new Location(((WorldBridge) this.entity.level).bridge$getWorld(), position.x, position.y + 2, position.z),
    //                3, 0, 0, 0, 0);
    //    }
    //}

    @Inject(method = "addPairing", at = @At("RETURN"))
    private void arclight$startTracking(ServerPlayer player, CallbackInfo ci) {
        if (!(this.entity instanceof ServerPlayer)) {
            MoveInterpolatorService.getInterpolator().startTracking((ServerEntity) (Object) this, player);
        }
    }

    @Inject(method = "removePairing", at = @At("HEAD"))
    private void arclight$stopTracking(ServerPlayer player, CallbackInfo ci) {
        if (!(this.entity instanceof ServerPlayer)) {
            MoveInterpolatorService.getInterpolator().stopTracking((ServerEntity) (Object) this, player);
        }
    }

    @Override
    public boolean bridge$syncPosition() {
        try {
            return this.positionSynched;
        } finally {
            this.positionSynched = false;
        }
    }

    @Override
    public boolean bridge$instantSyncPosition() {
        return this.entity.hasImpulse || this.entity.getEntityData().isDirty();
    }

    @Override
    public boolean bridge$instantSyncMotion() {
        try {
            return this.motionSynched || this.entity.hasImpulse || this.entity instanceof LivingEntity && ((LivingEntity) this.entity).isFallFlying();
        } finally {
            this.motionSynched = false;
        }
    }

    @Override
    public Entity bridge$getTrackingEntity() {
        return this.entity;
    }
}
