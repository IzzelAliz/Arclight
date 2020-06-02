package io.izzel.arclight.impl.mixin.v1_14.world.dimension;

import net.minecraft.world.World;
import net.minecraft.world.dimension.Dimension;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;

import java.util.function.BiFunction;

@Mixin(DimensionType.class)
public class DimensionTypeMixin_1_14 {

    protected void arclight$constructor(int idIn, String suffixIn, String directoryIn, BiFunction<World, DimensionType, ? extends Dimension> p_i49935_4_, boolean p_i49935_5_) {
        throw new RuntimeException();
    }

    public void arclight$constructor(int idIn, String suffixIn, String directoryIn, BiFunction<World, DimensionType, ? extends Dimension> p_i49935_4_, boolean p_i49935_5_, DimensionType type) {
        arclight$constructor(idIn, suffixIn, directoryIn, p_i49935_4_, p_i49935_5_);
        this.type = type;
    }

    private DimensionType type;

    public DimensionType getType() {
        return (type == null) ? (DimensionType) (Object) this : type;
    }
}
