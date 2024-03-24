package io.izzel.arclight.forge.mixin.core.world.level;

import io.izzel.arclight.common.bridge.core.world.spawner.BaseSpawnerBridge;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.SpawnData;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BaseSpawner.class)
public class BaseSpawnerMixin_Forge implements BaseSpawnerBridge {

    @Override
    public boolean bridge$forge$checkSpawnRules(Mob mob, ServerLevelAccessor level, MobSpawnType spawnType, SpawnData spawnData, boolean original) {
        var spawnEvent = new MobSpawnEvent.PositionCheck(mob, level, spawnType, null);
        MinecraftForge.EVENT_BUS.post(spawnEvent);
        var result = spawnEvent.getResult();
        if (result == Event.Result.DEFAULT) {
            return original;
        }
        return result == Event.Result.ALLOW;
    }

    @Override
    public void bridge$forge$finalizeSpawnerSpawn(Mob mob, ServerLevelAccessor level, DifficultyInstance difficulty, @Nullable SpawnGroupData spawnData, @Nullable CompoundTag spawnTag) {
        var event = net.minecraftforge.event.ForgeEventFactory.onFinalizeSpawnSpawner(mob, level, difficulty, spawnData, spawnTag, (BaseSpawner) (Object) this);
        if (event != null) {
            mob.finalizeSpawn(level, event.getDifficulty(), event.getSpawnType(), event.getSpawnData(), event.getSpawnTag());
        }
    }
}
