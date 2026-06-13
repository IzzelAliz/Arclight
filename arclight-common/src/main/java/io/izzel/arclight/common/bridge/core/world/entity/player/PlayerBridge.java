package io.izzel.arclight.common.bridge.core.world.entity.player;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import com.mojang.datafixers.util.Either;
import io.izzel.arclight.common.bridge.core.world.entity.LivingEntityBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.player.Player;
import org.bukkit.craftbukkit.entity.CraftHumanEntity;
import org.bukkit.event.entity.EntityExhaustionEvent;

public interface PlayerBridge extends LivingEntityBridge {

    boolean bridge$isFauxSleeping();

    @Override
    CraftHumanEntity bridge$getBukkitEntity();

    Either<Player.BedSleepingProblem, Unit> bridge$trySleep(BlockPos at, boolean force);

    void bridge$pushExhaustReason(EntityExhaustionEvent.ExhaustionReason reason);

    double bridge$platform$getBlockReach();

    default boolean bridge$platform$mayfly() {
        return ((Player) this).getAbilities().mayfly;
    }
}
