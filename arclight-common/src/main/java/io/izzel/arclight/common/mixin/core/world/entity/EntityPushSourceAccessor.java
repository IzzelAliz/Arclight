package io.izzel.arclight.common.mixin.core.world.entity;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Entity.class)
public interface EntityPushSourceAccessor {
    @Accessor(value = "arclight$attackPushSource", remap = false)
    Entity arclight$getAttackPushSource();

    @Accessor(value = "arclight$attackPushSource", remap = false)
    void arclight$setAttackPushSource(Entity source);
}
