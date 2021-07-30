package io.izzel.arclight.common.bridge.core.entity.player;

import com.mojang.datafixers.util.Either;
import io.izzel.arclight.common.bridge.core.entity.LivingEntityBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.player.Player;
import org.bukkit.craftbukkit.v.entity.CraftHumanEntity;
import org.bukkit.event.entity.EntityExhaustionEvent;

public interface PlayerEntityBridge extends LivingEntityBridge {

    boolean bridge$isFauxSleeping();

    @Override
    CraftHumanEntity bridge$getBukkitEntity();

    Either<Player.BedSleepingProblem, Unit> bridge$trySleep(BlockPos at, boolean force);

    void bridge$pushExhaustReason(EntityExhaustionEvent.ExhaustionReason reason);
}
