package io.izzel.arclight.common.mixin.core.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.TrapDoorBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TrapDoorBlock.class)
public class TrapDoorBlockMixin {

    @Redirect(method = "neighborChanged", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;isBlockPowered(Lnet/minecraft/util/math/BlockPos;)Z"))
    public boolean arclight$blockRedstone(World world, BlockPos pos, BlockState state, World worldIn, BlockPos blockPos, Block blockIn, BlockPos fromPos, boolean isMoving) {
        boolean flag = world.isBlockPowered(pos);
        if (flag != state.get(TrapDoorBlock.POWERED)) {
            org.bukkit.block.Block craftBlock = CraftBlock.at(world, pos);
            int power = craftBlock.getBlockPower();
            int oldPower = state.get(TrapDoorBlock.OPEN) ? 15 : 0;

            if (oldPower == 0 ^ power == 0 || blockIn.getDefaultState().canProvidePower()) {
                BlockRedstoneEvent event = new BlockRedstoneEvent(craftBlock, oldPower, power);
                Bukkit.getPluginManager().callEvent(event);
                return event.getNewCurrent() > 0;
            }
        }
        return flag;
    }
}
