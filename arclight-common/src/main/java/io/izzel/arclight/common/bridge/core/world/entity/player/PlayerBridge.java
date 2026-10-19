package io.izzel.arclight.common.bridge.core.world.entity.player;

import com.mojang.datafixers.util.Either;
import io.izzel.arclight.common.bridge.core.world.entity.LivingEntityBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Unit;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.bukkit.craftbukkit.entity.CraftHumanEntity;
import org.bukkit.event.entity.EntityExhaustionEvent;

public interface PlayerBridge extends LivingEntityBridge {

    boolean bridge$isFauxSleeping();

    void bridge$cancelCopperShear(net.minecraft.world.entity.Entity target);

    boolean bridge$isCopperShearCancelled(net.minecraft.world.entity.Entity target);

    /** Dispatch/refund only; false means Bukkit vetoed this exact item entity. */
    boolean bridge$callDropItemEvent(net.minecraft.world.entity.item.ItemEntity item, boolean traceItem);

    /** NeoForge dispatches at its toss helper's admission call, not Player.drop RETURN. */
    default boolean bridge$platform$dropEventAtReturn() {
        return true;
    }

    @Override
    CraftHumanEntity bridge$getBukkitEntity();

    Either<Player.BedSleepingProblem, Unit> bridge$trySleep(BlockPos at, boolean force);

    void bridge$pushExhaustReason(EntityExhaustionEvent.ExhaustionReason reason);

    double bridge$platform$getBlockReach();

    default boolean bridge$platform$mayfly() {
        return ((Player) this).getAbilities().mayfly;
    }

    default float bridge$platform$scaleDamage(DamageSource source, Player player, float amount, Difficulty difficulty) {
        if (!source.scalesWithDifficulty()) {
            return amount;
        }
        return switch (difficulty) {
            case EASY -> Math.min(amount / 2.0F + 1.0F, amount);
            case HARD -> amount * 3.0F / 2.0F;
            default -> amount;
        };
    }
}
