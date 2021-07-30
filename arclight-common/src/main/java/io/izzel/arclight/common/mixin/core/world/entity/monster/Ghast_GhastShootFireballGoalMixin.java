package io.izzel.arclight.common.mixin.core.world.entity.monster;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import io.izzel.arclight.common.bridge.core.entity.projectile.DamagingProjectileEntityBridge;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.level.Level;

@Mixin(targets = "net.minecraft.world.entity.monster.Ghast$GhastShootFireballGoal")
public abstract class Ghast_GhastShootFireballGoalMixin {

    @Shadow @Final private Ghast ghast;

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean arclight$setYaw(Level world, Entity entityIn) {
        ((DamagingProjectileEntityBridge) entityIn).bridge$setBukkitYield(this.ghast.getExplosionPower());
        return world.addFreshEntity(entityIn);
    }
}
