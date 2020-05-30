package io.izzel.arclight.common.mixin.core.world;

import io.izzel.arclight.common.bridge.world.IWorldWriterBridge;
import net.minecraft.entity.Entity;
import net.minecraft.world.IWorldWriter;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(IWorldWriter.class)
public interface IWorldWriterMixin extends IWorldWriterBridge {

    default boolean addEntity(Entity entity, CreatureSpawnEvent.SpawnReason reason) {
        return bridge$addEntity(entity, reason);
    }
}
