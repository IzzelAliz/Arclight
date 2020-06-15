package io.izzel.arclight.common.bridge.world.storage;

import net.minecraft.world.World;
import net.minecraft.world.dimension.Dimension;
import net.minecraft.world.dimension.DimensionType;
import org.bukkit.craftbukkit.v.map.CraftMapView;

import java.util.function.BiFunction;

public interface MapDataBridge {

    CraftMapView bridge$getMapView();

    DimensionType bridge$dimension(int id, String suffix, String dir, BiFunction<World, DimensionType, ? extends Dimension> provider, boolean skyLight);
}
