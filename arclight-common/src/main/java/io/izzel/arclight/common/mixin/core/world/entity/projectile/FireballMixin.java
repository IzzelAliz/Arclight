package io.izzel.arclight.common.mixin.core.world.entity.projectile;

import net.minecraft.world.entity.projectile.Fireball;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Fireball.class)
public abstract class FireballMixin extends AbstractHurtingProjectileMixin {

}
