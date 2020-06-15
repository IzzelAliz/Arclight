package io.izzel.arclight.common.mixin.core.world.dimension;

import io.izzel.arclight.common.bridge.world.dimension.DimensionBridge;
import net.minecraft.world.dimension.Dimension;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Dimension.class)
public abstract class DimensionMixin implements DimensionBridge {

    // @formatter:off
    @Accessor("type") public abstract DimensionType bridge$getDimensionManager();
    // @formatter:on

    public DimensionType getDimensionManager() {
        return bridge$getDimensionManager();
    }
}
