package io.izzel.arclight.common.mixin.core.world.level.block;

import io.izzel.arclight.common.mod.server.block.CauldronHooks;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.event.block.CauldronLevelChangeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CauldronBlock.class)
public class CauldronBlockMixin {

    @Redirect(method = "receiveStalactiteDrip", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean arclight$drip(Level level, BlockPos pos, BlockState state, BlockState old,
                                  @Share("arclight$dripCancelled") LocalBooleanRef cancelled) {
        boolean accepted = CauldronHooks.changeLevel(old, level, pos, state, null, CauldronLevelChangeEvent.ChangeReason.NATURAL_FILL);
        cancelled.set(!accepted);
        return accepted;
    }

    @WrapOperation(method = "receiveStalactiteDrip", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;gameEvent(Lnet/minecraft/core/Holder;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/gameevent/GameEvent$Context;)V"))
    private void arclight$dripGameEvent(Level level, Holder<GameEvent> event, BlockPos pos,
                                       GameEvent.Context context, Operation<Void> original,
                                       @Share("arclight$dripCancelled") LocalBooleanRef cancelled) {
        if (!cancelled.get()) {
            original.call(level, event, pos, context);
        }
    }
}
