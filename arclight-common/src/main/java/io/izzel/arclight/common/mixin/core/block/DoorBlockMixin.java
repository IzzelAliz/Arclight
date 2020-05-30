package io.izzel.arclight.common.mixin.core.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.properties.DoubleBlockHalf;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(DoorBlock.class)
public abstract class DoorBlockMixin {

    // @formatter:off
    @Shadow @Final public static EnumProperty<DoubleBlockHalf> HALF;
    @Shadow @Final public static BooleanProperty POWERED;
    @Shadow @Final public static BooleanProperty OPEN;
    @Shadow protected abstract void playSound(World worldIn, BlockPos pos, boolean isOpening);
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void neighborChanged(BlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving) {
        BlockPos blockPos = pos.offset(state.get(HALF) == DoubleBlockHalf.LOWER ? Direction.UP : Direction.DOWN);

        org.bukkit.block.Block bukkitBlock = CraftBlock.at(worldIn, pos);
        org.bukkit.block.Block blockTop = CraftBlock.at(worldIn, blockPos);

        int power = bukkitBlock.getBlockPower();
        int powerTop = blockTop.getBlockPower();
        if (powerTop > power) power = powerTop;
        int oldPower = state.get(DoorBlock.POWERED) ? 15 : 0;

        if (oldPower == 0 ^ power == 0) {
            BlockRedstoneEvent event = new BlockRedstoneEvent(bukkitBlock, oldPower, power);
            Bukkit.getPluginManager().callEvent(event);

            boolean flag = event.getNewCurrent() > 0;
            if (flag != state.get(OPEN)) {
                this.playSound(worldIn, pos, flag);
            }

            worldIn.setBlockState(pos, state.with(POWERED, flag).with(OPEN, flag), 2);
        }
    }
}
