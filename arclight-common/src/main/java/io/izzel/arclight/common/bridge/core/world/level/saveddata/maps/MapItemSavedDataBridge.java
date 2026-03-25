package io.izzel.arclight.common.bridge.core.world.level.saveddata.maps;

import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.bukkit.craftbukkit.v.map.CraftMapView;

import java.util.List;

public interface MapItemSavedDataBridge {

    CraftMapView bridge$getMapView();

    void bridge$setId(MapId id);

    List<MapItemSavedData.HoldingPlayer> bridge$getCarriedBy();
}
