package io.izzel.arclight.common.bridge.core.world.entity.npc;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.jetbrains.annotations.Nullable;

public interface VillagerBridge {

    @Nullable
    ZombieVillager bridge$zombify(ServerLevel level, BlockPos blockPosition, boolean silent, CreatureSpawnEvent.SpawnReason spawnReason);

    default boolean bridge$forge$canZombify() {
        return true;
    }

    default void bridge$forge$finalizeZombifySpawn(ServerLevel level, ZombieVillager zombie) {
        zombie.finalizeSpawn(level, level.getCurrentDifficultyAt(zombie.blockPosition()), EntitySpawnReason.CONVERSION, new Zombie.ZombieGroupData(false, true));
    }
}
