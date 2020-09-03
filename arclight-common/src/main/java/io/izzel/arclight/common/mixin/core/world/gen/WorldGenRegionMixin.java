package io.izzel.arclight.common.mixin.core.world.gen;

import io.izzel.arclight.common.bridge.world.WorldBridge;
import net.minecraft.entity.Entity;
import net.minecraft.world.gen.WorldGenRegion;
import net.minecraft.world.server.ServerWorld;
import org.bukkit.craftbukkit.v.CraftWorld;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(WorldGenRegion.class)
public abstract class WorldGenRegionMixin implements WorldBridge {

    // @formatter:off
    @Shadow public abstract boolean addEntity(Entity entityIn);
    @Shadow @Final private ServerWorld world;
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

    @Override
    public CraftWorld bridge$getWorld() {
        return ((WorldBridge) this.world).bridge$getWorld();
    }

    @Override
    public CreatureSpawnEvent.SpawnReason bridge$getAddEntityReason() {
        return CreatureSpawnEvent.SpawnReason.DEFAULT;
    }
}
