package io.izzel.arclight.common.mixin.core.world.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Access only the metadata that PowderSnowBlock must resend after an inside-block veto. */
@Mixin(Entity.class)
public interface EntityInsideBlockFrozenDataAccessor {

    @Accessor("DATA_TICKS_FROZEN")
    static EntityDataAccessor<Integer> arclight$insideBlockFrozenTicks() {
        throw new AssertionError();
    }
}
