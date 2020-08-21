package io.izzel.arclight.common.bridge.world;

import net.minecraft.entity.Entity;
import org.bukkit.event.entity.CreatureSpawnEvent;

public interface IWorldWriterBridge {

    boolean bridge$addEntity(Entity entity, CreatureSpawnEvent.SpawnReason reason);

    void bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason reason);

    CreatureSpawnEvent.SpawnReason bridge$getAddEntityReason();
}
