package io.izzel.arclight.common.mixin.core.world.spawner;

import io.izzel.arclight.common.bridge.world.spawner.WorldEntitySpawnerBridge;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MobEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.spawner.WorldEntitySpawner;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(WorldEntitySpawner.EntityDensityManager.class)
public abstract class WorldEntitySpawner_EntityDensityManagerMixin implements WorldEntitySpawnerBridge.EntityDensityManagerBridge {

    // @formatter:off
    @Shadow protected abstract void func_234990_a_(MobEntity p_234990_1_, IChunk p_234990_2_);
    @Shadow @Final private int field_234981_a_;
    @Shadow @Final private Object2IntOpenHashMap<EntityClassification> field_234982_b_;
    @Shadow protected abstract boolean func_234989_a_(EntityType<?> p_234989_1_, BlockPos p_234989_2_, IChunk p_234989_3_);
    // @formatter:on

    @Override
    public boolean bridge$canSpawn(EntityType<?> entityType, BlockPos pos, IChunk chunk) {
        return this.func_234989_a_(entityType, pos, chunk);
    }

    @Override
    public void bridge$updateDensity(MobEntity mobEntity, IChunk chunk) {
        this.func_234990_a_(mobEntity, chunk);
    }

    @Override
    public boolean bridge$canSpawn(EntityClassification classification, int limit) {
        int i = limit * this.field_234981_a_ / 289;
        return this.field_234982_b_.getInt(classification) < i;
    }
}
