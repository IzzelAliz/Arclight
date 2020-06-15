package io.izzel.arclight.common.mixin.v1_15.world.storage;

import io.izzel.arclight.common.bridge.world.storage.MapDataBridge;
import net.minecraft.world.World;
import net.minecraft.world.dimension.Dimension;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.storage.MapData;
import org.spongepowered.asm.mixin.Mixin;

import java.util.function.BiFunction;

@Mixin(MapData.class)
public abstract class MapDataMixin_1_15 implements MapDataBridge {

    @Override
    public DimensionType bridge$dimension(int id, String suffix, String dir, BiFunction<World, DimensionType, ? extends Dimension> provider, boolean skyLight) {
        return new DimensionType(id, suffix, dir, provider, skyLight, null, null, null);
    }
}
