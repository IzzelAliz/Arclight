package io.izzel.arclight.common.mixin.core.world.gen;

import io.izzel.arclight.common.bridge.world.WorldBridge;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.gen.WorldGenRegion;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.gen.feature.structure.StructureStart;
import net.minecraft.world.server.ServerWorld;
import org.bukkit.craftbukkit.v.CraftWorld;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.stream.Stream;

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

    /**
     * @author IzzelAliz
     * @reason MC-199487
     */
    @Overwrite
    public Stream<? extends StructureStart<?>> func_241827_a(SectionPos p_241827_1_, Structure<?> p_241827_2_) {
        return this.world.func_241112_a_().getStructureManager((WorldGenRegion) (Object) this).func_235011_a_(p_241827_1_, p_241827_2_);
    }
}
