package io.izzel.arclight.common.mixin.core.world.level;

import io.izzel.arclight.common.bridge.core.world.level.IWorldWriterBridge;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelWriter;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LevelWriter.class)
public interface LevelWriterMixin extends IWorldWriterBridge {

    default boolean addFreshEntity(Entity entity, CreatureSpawnEvent.SpawnReason reason) {
        return bridge$addEntity(entity, reason);
    }
}
