package io.izzel.arclight.common.bridge.core.world.level.saveddata.maps;

import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.bukkit.craftbukkit.map.CraftMapView;

import java.util.List;
import java.util.UUID;

public interface MapItemSavedDataBridge {

    UUID bridge$updateUUID();

    void bridge$resolveDimension(long least, long most);

    CraftMapView bridge$getMapView();

    void bridge$setId(MapId id);

    MapId bridge$getId();

    UUID bridge$getUniqueId();

    void bridge$setUniqueId(UUID uniqueId);

    void bridge$markCanvasDirty(int x, int y);

    List<MapItemSavedData.HoldingPlayer> bridge$getCarriedBy();
}
