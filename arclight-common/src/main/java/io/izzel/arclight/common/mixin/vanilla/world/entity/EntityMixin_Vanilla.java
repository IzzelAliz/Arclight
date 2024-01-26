package io.izzel.arclight.common.mixin.vanilla.world.entity;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Entity.class)
public abstract class EntityMixin_Vanilla implements EntityBridge {

    // @formatter:off
    @Shadow public abstract int getId();
    @Shadow public abstract Vec3 position();
    @Shadow public abstract void unRide();
    @Shadow public abstract void discard();
    @Shadow public abstract Level level();
    // @formatter:on
}
