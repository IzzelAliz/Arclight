package io.izzel.arclight.common.mixin.core.world.entity.projectile.windcharge;

import io.izzel.arclight.common.mixin.core.world.entity.projectile.AbstractHurtingProjectileMixin;
import net.minecraft.world.entity.projectile.windcharge.AbstractWindCharge;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractWindCharge.class)
public abstract class AbstractWindChargeMixin extends AbstractHurtingProjectileMixin {

    @Inject(method = "onHitBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/windcharge/AbstractWindCharge;discard()V"))
    private void arclight$hitBlock(BlockHitResult blockHitResult, CallbackInfo ci) {
        bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.HIT);
    }

    @Inject(method = "onHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/windcharge/AbstractWindCharge;discard()V"))
    private void arclight$hit(HitResult hitResult, CallbackInfo ci) {
        bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.HIT);
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/windcharge/AbstractWindCharge;discard()V"))
    private void arclight$outOfWorld(CallbackInfo ci) {
        bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.OUT_OF_WORLD);
    }
}
