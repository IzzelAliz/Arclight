package io.izzel.arclight.common.mixin.core.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TrapDoorBlock.class)
public class TrapDoorBlockMixin {

    @Redirect(method = "neighborChanged", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;hasNeighborSignal(Lnet/minecraft/core/BlockPos;)Z"))
    public boolean arclight$blockRedstone(Level world, BlockPos pos, BlockState state, Level worldIn, BlockPos blockPos, Block blockIn, BlockPos fromPos, boolean isMoving) {
        boolean flag = world.hasNeighborSignal(pos);
        if (flag != state.getValue(TrapDoorBlock.POWERED)) {
            org.bukkit.block.Block craftBlock = CraftBlock.at(world, pos);
            int power = craftBlock.getBlockPower();
            int oldPower = state.getValue(TrapDoorBlock.OPEN) ? 15 : 0;

            if (oldPower == 0 ^ power == 0 || blockIn.defaultBlockState().isSignalSource()) {
                BlockRedstoneEvent event = new BlockRedstoneEvent(craftBlock, oldPower, power);
                Bukkit.getPluginManager().callEvent(event);
                return event.getNewCurrent() > 0;
            }
        }
        return flag;
    }
}
