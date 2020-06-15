package io.izzel.arclight.impl.mixin.v1_14.core.entity;

import io.izzel.arclight.common.bridge.entity.MobEntityBridge;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;

@Mixin(MobEntity.class)
public abstract class MobEntityMixin_1_14 extends LivingEntityMixin_1_14 implements MobEntityBridge {

    // @formatter:off
    @Shadow @Nullable public abstract LivingEntity getAttackTarget();
    @Shadow protected void updateAITasks() {}
    // @formatter:on
}
