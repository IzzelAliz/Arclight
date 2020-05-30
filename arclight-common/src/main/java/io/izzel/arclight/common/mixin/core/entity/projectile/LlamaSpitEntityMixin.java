package io.izzel.arclight.common.mixin.core.entity.projectile;

import io.izzel.arclight.common.mixin.core.entity.EntityMixin;
import net.minecraft.entity.projectile.LlamaSpitEntity;
import net.minecraft.util.math.RayTraceResult;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LlamaSpitEntity.class)
public abstract class LlamaSpitEntityMixin extends EntityMixin {

    @Inject(method = "onHit", at = @At("HEAD"))
    private void arclight$projectileHit(RayTraceResult result, CallbackInfo ci) {
        CraftEventFactory.callProjectileHitEvent((LlamaSpitEntity)(Object)this, result);
    }
}
