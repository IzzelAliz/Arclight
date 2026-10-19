package io.izzel.arclight.common.mixin.core.world.waypoints;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.server.level.ServerPlayerBridge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.waypoints.WaypointTransmitter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WaypointTransmitter.class)
public interface WaypointTransmitterMixin {

    @Inject(method = "doesSourceIgnoreReceiver(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/server/level/ServerPlayer;)Z", at = @At("HEAD"), cancellable = true, require = 1)
    private static void arclight$visibility(LivingEntity source, ServerPlayer receiver, CallbackInfoReturnable<Boolean> cir) {
        if (!((ServerPlayerBridge) receiver).bridge$getBukkitEntity().canSee(((EntityBridge) source).bridge$getBukkitEntity())) {
            cir.setReturnValue(true);
        }
    }
}
