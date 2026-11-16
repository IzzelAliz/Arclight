package io.izzel.arclight.common.mixin.core.world.level.portal;

import io.izzel.arclight.common.bridge.core.world.level.portal.DimensionTransitionBridge;
import io.izzel.arclight.common.mod.mixins.annotation.CreateConstructor;
import io.izzel.arclight.common.mod.mixins.annotation.ShadowConstructor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(TeleportTransition.class)
public class DimensionTransitionMixin implements DimensionTransitionBridge {

    @ShadowConstructor
    public void arclight$constructor(ServerLevel newLevel, Vec3 pos, Vec3 speed, float yRot, float xRot, boolean missingRespawnBlock, boolean asPassenger, java.util.Set<net.minecraft.world.entity.Relative> relatives, TeleportTransition.PostTeleportTransition postTeleportTransition) {
        throw new RuntimeException();
    }

    @CreateConstructor
    public void arclight$constructor(ServerLevel newLevel, Vec3 pos, Vec3 speed, float yRot, float xRot, boolean missingRespawnBlock, TeleportTransition.PostTeleportTransition postTeleportTransition, PlayerTeleportEvent.TeleportCause cause) {
        arclight$constructor(newLevel, pos, speed, yRot, xRot, missingRespawnBlock, false, java.util.Set.of(), postTeleportTransition);
        this.cause = cause;
    }

    @ShadowConstructor
    public void arclight$constructor(ServerLevel serverLevel, Vec3 vec3, Vec3 vec32, float f, float g, TeleportTransition.PostTeleportTransition postTeleportTransition) {
        throw new RuntimeException();
    }

    @CreateConstructor
    public void arclight$constructor(ServerLevel serverLevel, Vec3 vec3, Vec3 vec32, float f, float g, TeleportTransition.PostTeleportTransition postTeleportTransition, PlayerTeleportEvent.TeleportCause cause) {
        arclight$constructor(serverLevel, vec3, vec32, f, g, postTeleportTransition);
        this.cause = cause;
    }

    @Unique private PlayerTeleportEvent.TeleportCause cause; // Paper added

    @Unique private boolean arclight$positionRider;

    @Override
    public void bridge$setTeleportCause(PlayerTeleportEvent.TeleportCause cause) {
        this.cause = cause;
    }

    @Override
    public PlayerTeleportEvent.TeleportCause bridge$getTeleportCause() {
        return this.cause == null ? PlayerTeleportEvent.TeleportCause.UNKNOWN : this.cause;
    }

    @Override
    public void bridge$setPositionRider(boolean value) {
        this.arclight$positionRider = value;
    }

    @Override
    public boolean bridge$isPositionRider() {
        return this.arclight$positionRider;
    }

    @com.llamalad7.mixinextras.injector.ModifyReturnValue(method = {
        "withRotation(FF)Lnet/minecraft/world/level/portal/TeleportTransition;",
        "withPosition(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/level/portal/TeleportTransition;",
        "transitionAsPassenger()Lnet/minecraft/world/level/portal/TeleportTransition;"
    }, at = @org.spongepowered.asm.mixin.injection.At("RETURN"))
    private TeleportTransition arclight$copyMetadata(TeleportTransition result) {
        var bridge = (DimensionTransitionBridge) (Object) result;
        bridge.bridge$setTeleportCause(this.cause);
        bridge.bridge$setPositionRider(this.arclight$positionRider);
        return result;
    }
}
