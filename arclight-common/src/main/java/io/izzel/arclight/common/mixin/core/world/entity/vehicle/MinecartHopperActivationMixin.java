package io.izzel.arclight.common.mixin.core.world.entity.vehicle;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.server.MinecraftServerBridge;
import net.minecraft.world.entity.vehicle.minecart.MinecartHopper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecartHopper.class)
public abstract class MinecartHopperActivationMixin {
    private void arclight$immunize() {
        var entity = (MinecartHopper) (Object) this;
        if (entity.level().isClientSide()) return;
        EntityBridge bridge = (EntityBridge) entity;
        long tick = ((MinecraftServerBridge) entity.level().getServer()).bridge$getCurrentTick();
        bridge.bridge$setActivatedImmunityTick(Math.max(bridge.bridge$getActivatedImmunityTick(), tick + 20));
    }
    @Inject(method = "activateMinecart", at = @At("RETURN"))
    private void arclight$activation(CallbackInfo ci) { this.arclight$immunize(); }
    @Inject(method = "suckInItems", at = @At("RETURN"))
    private void arclight$pickup(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) this.arclight$immunize();
    }
}
