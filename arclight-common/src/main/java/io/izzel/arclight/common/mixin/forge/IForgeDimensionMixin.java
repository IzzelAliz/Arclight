package io.izzel.arclight.common.mixin.forge;

import io.izzel.arclight.common.bridge.world.dimension.DimensionTypeBridge;
import net.minecraft.world.World;
import net.minecraft.world.dimension.Dimension;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.common.extensions.IForgeDimension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = IForgeDimension.class, remap = false)
public interface IForgeDimensionMixin {

    // @formatter:off
    @Shadow Dimension getDimension();
    @Shadow World getWorld();
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    default boolean isDaytime() {
        return ((DimensionTypeBridge) getDimension().getType()).bridge$getType() == DimensionType.OVERWORLD
            && getWorld().getSkylightSubtracted() < 4;
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    default boolean shouldMapSpin(String entity, double x, double z, double rotation) {
        return ((DimensionTypeBridge) getDimension().getType()).bridge$getType() == DimensionType.THE_NETHER;
    }
}
