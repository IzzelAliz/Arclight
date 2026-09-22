package io.izzel.arclight.common.mixin.core.world.entity.projectile;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownLingeringPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.LingeringPotionSplashEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(ThrownLingeringPotion.class)
public abstract class ThrownLingeringPotionMixin extends ThrownPotionMixin {

    @Inject(method = "onHitAsPotion", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private void arclight$makeCloud(ServerLevel level, ItemStack stack, HitResult hitResult, CallbackInfo ci, AreaEffectCloud entity) {
        LingeringPotionSplashEvent event = CraftEventFactory.callLingeringPotionSplashEvent((ThrownLingeringPotion) (Object) this, hitResult, entity);
        if (event.isCancelled() || entity.isRemoved()) {
            ci.cancel();
            entity.discard();
        }
    }
}
