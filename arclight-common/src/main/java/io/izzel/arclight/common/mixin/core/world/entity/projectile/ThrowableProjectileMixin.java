package io.izzel.arclight.common.mixin.core.world.entity.projectile;

import net.minecraft.world.entity.projectile.ThrowableProjectile;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ThrowableProjectile.class)
public abstract class ThrowableProjectileMixin extends ProjectileMixin {
}
