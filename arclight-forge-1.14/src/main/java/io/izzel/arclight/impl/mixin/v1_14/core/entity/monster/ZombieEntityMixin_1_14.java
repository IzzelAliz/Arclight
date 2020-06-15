package io.izzel.arclight.impl.mixin.v1_14.core.entity.monster;

import io.izzel.arclight.impl.mixin.v1_14.core.entity.MobEntityMixin_1_14;
import net.minecraft.entity.monster.ZombieEntity;
import net.minecraft.util.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ZombieEntity.class)
public abstract class ZombieEntityMixin_1_14 extends MobEntityMixin_1_14 {

    // @formatter:off
    @Shadow public boolean attackEntityFrom(DamageSource source, float amount) { return false; }
    // @formatter:on
}
