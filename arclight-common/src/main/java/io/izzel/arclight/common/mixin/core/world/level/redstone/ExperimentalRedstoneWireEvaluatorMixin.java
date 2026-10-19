package io.izzel.arclight.common.mixin.core.world.level.redstone;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.redstone.ExperimentalRedstoneWireEvaluator;
import net.minecraft.world.level.redstone.Orientation;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ExperimentalRedstoneWireEvaluator.class)
public class ExperimentalRedstoneWireEvaluatorMixin {
    @ModifyExpressionValue(method = "updatePowerStrength", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"),
        require = 1, expect = 1, allow = 1)
    private BlockState arclight$event(BlockState state, Level level, BlockPos initialPos, BlockState ignored,
            Orientation orientation, boolean shapeUpdates, @Local(index = 10) BlockPos pos,
            @Local(index = 12) LocalIntRef newPower, @Share("arclight$entryPower") LocalIntRef oldPower) {
        int before = ignored.getValue(RedStoneWireBlock.POWER);
        oldPower.set(before);
        if (before != newPower.get()) {
            BlockRedstoneEvent event = new BlockRedstoneEvent(CraftBlock.at(level, pos), before, newPower.get());
            Bukkit.getPluginManager().callEvent(event);
            newPower.set(event.getNewCurrent());
        }
        return state;
    }

    @WrapOperation(method = "updatePowerStrength", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/block/state/BlockState;getValue(Lnet/minecraft/world/level/block/state/properties/Property;)Ljava/lang/Comparable;"),
        require = 1, expect = 1, allow = 1)
    private Comparable<?> arclight$comparison(BlockState state, Property<?> property, Operation<Comparable<?>> original,
            @Share("arclight$entryPower") LocalIntRef oldPower) {
        // Preserve peer participation at the native read, but use the selected fixed comparison baseline.
        original.call(state, property);
        return oldPower.get();
    }
}
