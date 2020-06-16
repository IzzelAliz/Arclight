package io.izzel.arclight.common.mixin.core.world.gen;

import io.izzel.arclight.common.bridge.world.IWorldWriterBridge;
import net.minecraft.entity.Entity;
import net.minecraft.world.gen.WorldGenRegion;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(WorldGenRegion.class)
public abstract class WorldGenRegionMixin implements IWorldWriterBridge {

    // @formatter:off
    @Shadow public abstract boolean addEntity(Entity entityIn);
    // @formatter:on

    public boolean addEntity(Entity entity, CreatureSpawnEvent.SpawnReason reason) {
        return this.addEntity(entity);
    }

    @Override
    public boolean bridge$addEntity(Entity entity, CreatureSpawnEvent.SpawnReason reason) {
        return addEntity(entity, reason);
    }

    @Override
    public void bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason reason) {
    }
}
