package io.izzel.arclight.common.mixin.core.world.gen;

import io.izzel.arclight.common.bridge.core.world.WorldBridge;
import org.bukkit.craftbukkit.v.CraftWorld;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.stream.Stream;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.structure.StructureStart;

@Mixin(WorldGenRegion.class)
public abstract class WorldGenRegionMixin implements WorldBridge {

    // @formatter:off
    @Shadow public abstract boolean addFreshEntity(Entity entityIn);
    @Shadow @Final private ServerLevel level;
    // @formatter:on

    public boolean addEntity(Entity entity, CreatureSpawnEvent.SpawnReason reason) {
        return this.addFreshEntity(entity);
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
        return ((WorldBridge) this.level).bridge$getWorld();
    }

    @Override
    public CreatureSpawnEvent.SpawnReason bridge$getAddEntityReason() {
        return CreatureSpawnEvent.SpawnReason.DEFAULT;
    }

    /**
     * @author IzzelAliz
     * @reason MC-199487
     */
    @Overwrite
    public Stream<? extends StructureStart<?>> startsForFeature(SectionPos p_241827_1_, StructureFeature<?> p_241827_2_) {
        return this.level.structureFeatureManager().forWorldGenRegion((WorldGenRegion) (Object) this).startsForFeature(p_241827_1_, p_241827_2_);
    }
}
