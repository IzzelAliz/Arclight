package io.izzel.arclight.common.mixin.core.world.entity.vehicle;

import io.izzel.arclight.common.bridge.core.world.entity.vehicle.AbstractMinecartBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.MinecartBehavior;
import net.minecraft.world.entity.vehicle.minecart.NewMinecartBehavior;
import net.minecraft.world.entity.vehicle.minecart.OldMinecartBehavior;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({NewMinecartBehavior.class, OldMinecartBehavior.class})
public abstract class MinecartBehaviorMixin extends MinecartBehavior {
    protected MinecartBehaviorMixin(AbstractMinecart minecart) {
        super(minecart);
    }

    @com.llamalad7.mixinextras.injector.ModifyExpressionValue(method = "getSlowdownFactor", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/vehicle/minecart/AbstractMinecart;isVehicle()Z"))
    private boolean arclight$occupiedOrNoSlowdown(boolean original) {
        return original || !((AbstractMinecartBridge) this.minecart).bridge$isSlowWhenEmpty();
    }

    @Inject(method = "getMaxSpeed", at = @At("HEAD"), cancellable = true)
    private void arclight$maxSpeedOverride(ServerLevel level, CallbackInfoReturnable<Double> cir) {
        Double maxSpeed = ((AbstractMinecartBridge) this.minecart).bridge$getMaxSpeedOverride();
        if (maxSpeed != null) {
            cir.setReturnValue(this.minecart.isInWater() ? maxSpeed / 2.0D : maxSpeed);
        }
    }
}
