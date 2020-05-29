package io.izzel.arclight.bridge.entity.player;

import com.mojang.datafixers.util.Either;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Unit;
import net.minecraft.util.math.BlockPos;
import org.bukkit.craftbukkit.v.entity.CraftHumanEntity;
import io.izzel.arclight.bridge.entity.LivingEntityBridge;

public interface PlayerEntityBridge extends LivingEntityBridge {

    boolean bridge$isFauxSleeping();

    String bridge$getSpawnWorld();

    @Override
    CraftHumanEntity bridge$getBukkitEntity();

    Either<PlayerEntity.SleepResult, Unit> bridge$trySleep(BlockPos at, boolean force);
}
