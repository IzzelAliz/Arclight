package io.izzel.arclight.common.mixin.core.world.level.block;

import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.DefaultRedstoneWireEvaluator;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(DefaultRedstoneWireEvaluator.class)
public abstract class RedStoneWireBlockMixin {

    @Decorate(method = "updatePowerStrength", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/redstone/DefaultRedstoneWireEvaluator;calculateTargetStrength(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)I"))
    private int arclight$blockRedstone(DefaultRedstoneWireEvaluator evaluator, Level world, BlockPos pos,
            @Local(ordinal = 0) BlockState oldState) throws Throwable {
        int i = (int) DecorationOps.callsite().invoke(evaluator, world, pos);
        int oldPower = oldState.getValue(RedStoneWireBlock.POWER);
        if (oldPower != i) {
            BlockRedstoneEvent event = new BlockRedstoneEvent(CraftBlock.at(world, pos), oldPower, i);
            Bukkit.getPluginManager().callEvent(event);
            i = event.getNewCurrent();
        }
        return i;
    }
}
