package io.izzel.arclight.neoforge.mixin.core.world.level;

import io.izzel.arclight.common.bridge.core.world.spawner.BaseSpawnerBridge;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.SpawnData;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BaseSpawner.class)
public abstract class BaseSpawnerMixin_NeoForge implements BaseSpawnerBridge {

    @Override
    public boolean bridge$forge$checkSpawnRules(Mob mob, ServerLevelAccessor level, MobSpawnType spawnType, SpawnData spawnData, boolean original) {
        var event = new MobSpawnEvent.PositionCheck(mob, level, spawnType, null);
        NeoForge.EVENT_BUS.post(event);
        if (event.getResult() == Event.Result.DEFAULT) {
            return original;
        }
        return event.getResult() == Event.Result.ALLOW;
    }

    @Override
    public void bridge$forge$finalizeSpawnerSpawn(Mob mob, ServerLevelAccessor level, DifficultyInstance difficulty, @Nullable SpawnGroupData spawnData, @Nullable CompoundTag spawnTag) {
        var event = EventHooks.onFinalizeSpawnSpawner(mob, level, difficulty, spawnData, spawnTag, (BaseSpawner) (Object) this);
        if (event != null) {
            mob.finalizeSpawn(level, event.getDifficulty(), event.getSpawnType(), event.getSpawnData(), event.getSpawnTag());
        }
    }
}
