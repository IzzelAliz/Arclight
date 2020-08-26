package io.izzel.arclight.common.mixin.core.entity.item;

import io.izzel.arclight.common.bridge.world.WorldBridge;
import io.izzel.arclight.common.mixin.core.entity.projectile.ThrowableEntityMixin;
import net.minecraft.entity.item.EnderPearlEntity;
import net.minecraft.util.math.RayTraceResult;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnderPearlEntity.class)
public abstract class EnderPearlEntityMixin extends ThrowableEntityMixin {

    @Inject(method = "onImpact", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;addEntity(Lnet/minecraft/entity/Entity;)Z"))
    private void arclight$spawnEndermite(RayTraceResult result, CallbackInfo ci) {
        ((WorldBridge) this.world).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.ENDER_PEARL);
    }
}
