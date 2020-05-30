package io.izzel.arclight.common.mixin.core.entity.monster;

import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.GhastEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import io.izzel.arclight.common.bridge.entity.projectile.DamagingProjectileEntityBridge;

@Mixin(targets = "net.minecraft.entity.monster.GhastEntity.FireballAttackGoal")
public abstract class GhastEntity_FireballAttackGoalMixin {

    @Shadow @Final private GhastEntity parentEntity;

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;addEntity(Lnet/minecraft/entity/Entity;)Z"))
    private boolean arclight$setYaw(World world, Entity entityIn) {
        ((DamagingProjectileEntityBridge) entityIn).bridge$setBukkitYield(this.parentEntity.getFireballStrength());
        return world.addEntity(entityIn);
    }
}
