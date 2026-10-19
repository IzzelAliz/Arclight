package io.izzel.arclight.common.mixin.core.world.level.block;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import io.izzel.arclight.common.bridge.core.world.level.LevelAccessorBridge;
import io.izzel.arclight.common.mod.server.event.ArclightEventFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.block.CraftBlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LiquidBlock.class)
public class LiquidBlockMixin {

    @WrapOperation(method = "shouldSpreadLiquid", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    public boolean arclight$blockForm(Level world, BlockPos pos, BlockState newState, Operation<Boolean> original,
                                     @Share("arclight$fizz") LocalBooleanRef fizz) {
        if (LevelAccessorBridge.from(world) instanceof LevelAccessorBridge bridge) {
            final var event = ArclightEventFactory.callBlockFormEvent(bridge.bridge$getMinecraftWorld(), pos, newState, 3, null);
            if (event != null) {
                if (event.isCancelled()) {
                    fizz.set(false);
                    return false;
                }
                newState = ((CraftBlockState) event.getNewState()).getHandle();
            }
        }
        // Keep the existing setter-result gate; event acceptance is a separate policy.
        boolean result = original.call(world, pos, newState);
        fizz.set(result);
        return result;
    }

    @WrapOperation(method = "shouldSpreadLiquid", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/LiquidBlock;fizz(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;)V"))
    public void arclight$fizz(LiquidBlock block, LevelAccessor world, BlockPos pos, Operation<Void> original,
                              @Share("arclight$fizz") LocalBooleanRef fizz) {
        if (fizz.get()) {
            original.call(block, world, pos);
        }
    }
}
