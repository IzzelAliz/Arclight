package io.izzel.arclight.common.bridge.core.world.storage;

import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.bukkit.craftbukkit.v.map.CraftMapView;

import java.util.List;

public interface MapDataBridge {

    CraftMapView bridge$getMapView();

    void bridge$setId(String id);

    List<MapItemSavedData.HoldingPlayer> bridge$getCarriedBy();
}
