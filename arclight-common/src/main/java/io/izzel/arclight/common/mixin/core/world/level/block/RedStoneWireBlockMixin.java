package io.izzel.arclight.common.mixin.core.world.level.block;

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
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(DefaultRedstoneWireEvaluator.class)
public abstract class RedStoneWireBlockMixin {

    @Redirect(method = "updatePowerStrength", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    public boolean arclight$blockRedstone(Level world, BlockPos pos, BlockState newState, int flags, Level level, BlockPos blockPos, BlockState oldState) {
        int i = newState.getValue(RedStoneWireBlock.POWER);
        int oldPower = oldState.getValue(RedStoneWireBlock.POWER);
        if (oldPower != i) {
            BlockRedstoneEvent event = new BlockRedstoneEvent(CraftBlock.at(world, pos), oldPower, i);
            Bukkit.getPluginManager().callEvent(event);
            i = event.getNewCurrent();
        }
        return world.setBlock(pos, newState.setValue(RedStoneWireBlock.POWER, i), flags);
    }
}
