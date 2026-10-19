package io.izzel.arclight.common.mixin.vanilla.world.level.block;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.izzel.arclight.api.ArclightPlatform;
import io.izzel.arclight.common.mod.mixins.annotation.OnlyInPlatform;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.TNTPrimeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Vanilla-signature burn bridge, shared by plain Vanilla and Fabric. */
@OnlyInPlatform({ArclightPlatform.VANILLA, ArclightPlatform.FABRIC})
@Mixin(FireBlock.class)
public abstract class FireBlockMixin_Vanilla {

    /** Handoff belongs to one wrapped tick call and may be claimed only once. */
    @Unique private static final ThreadLocal<FireBurnContext> arclight$fireBurnContext = new ThreadLocal<>();

    @Unique
    private static final class FireBurnContext {
        private final Level level;
        private final BlockPos targetPos;
        private final BlockPos sourcePos;
        private boolean claimed;

        private FireBurnContext(Level level, BlockPos targetPos, BlockPos sourcePos) {
            this.level = level;
            this.targetPos = targetPos;
            this.sourcePos = sourcePos;
        }
    }

    /**
     * Vanilla tick has six native target-burn call sites. Hand off only their actual fire
     * position, snapshotting both positions; always restore a parent frame on re-entry.
     */
    @WrapOperation(
        method = "tick(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/FireBlock;checkBurnOut(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;ILnet/minecraft/util/RandomSource;I)V"),
        require = 6, expect = 6, allow = 6
    )
    private void arclight$wrapBurnTarget(FireBlock fire, Level level, BlockPos targetPos, int chance,
            RandomSource random, int age, Operation<Void> original,
            @Local(argsOnly = true) BlockPos sourcePos) {
        FireBurnContext previous = arclight$fireBurnContext.get();
        FireBurnContext current = new FireBurnContext(level, targetPos.immutable(), sourcePos.immutable());
        arclight$fireBurnContext.set(current);
        try {
            // Preserve the native call and its RNG stream exactly once.
            original.call(fire, level, targetPos, chance, random, age);
        } finally {
            if (previous == null) {
                arclight$fireBurnContext.remove();
            } else {
                arclight$fireBurnContext.set(previous);
            }
        }
    }

    /** Claim before dispatch: direct listener re-entry cannot inherit this source/call. */
    @Unique
    private static BlockPos arclight$claimBurnSource(Level level, BlockPos targetPos) {
        FireBurnContext context = arclight$fireBurnContext.get();
        if (context == null || context.claimed || context.level != level
                || !context.targetPos.equals(targetPos)) {
            return null;
        }
        context.claimed = true;
        return context.sourcePos;
    }

    /**
     * In the vanilla body, nextInt ordinal 1 is the burnout draw: ordinal 0 already succeeded,
     * and oldState has been read. This gate precedes burnout RNG, block write/remove, and the
     * native TNT prime tail. Cancellation returns from only this target's checkBurnOut call.
     */
    @Inject(
        method = "checkBurnOut(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;ILnet/minecraft/util/RandomSource;I)V",
        at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/util/RandomSource;nextInt(I)I"),
        cancellable = true,
        require = 1, expect = 1, allow = 1
    )
    private void arclight$blockBurn(Level level, BlockPos targetPos, int chance,
            RandomSource random, int age, CallbackInfo ci, @Local BlockState oldState) {
        // Claim before any plugin callback; a direct nested checkBurnOut has no invocation owner.
        BlockPos sourcePos = arclight$claimBurnSource(level, targetPos);
        if (sourcePos == null) {
            return;
        }

        BlockBurnEvent burnEvent = new BlockBurnEvent(
            CraftBlock.at(level, targetPos), CraftBlock.at(level, sourcePos));
        Bukkit.getPluginManager().callEvent(burnEvent);
        if (burnEvent.isCancelled()) {
            ci.cancel();
            return;
        }

        if (oldState.getBlock() instanceof TntBlock
                && !CraftEventFactory.callTNTPrimeEvent(level, targetPos,
                    TNTPrimeEvent.PrimeCause.FIRE, null, sourcePos)) {
            ci.cancel();
        }
    }
}
