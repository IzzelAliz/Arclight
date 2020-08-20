package io.izzel.arclight.common.mixin.core.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CommandBlockBlock;
import net.minecraft.tileentity.CommandBlockTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(CommandBlockBlock.class)
public abstract class CommandBlockBlockMixin {

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void neighborChanged(BlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving) {
        if (!worldIn.isRemote) {
            TileEntity tileentity = worldIn.getTileEntity(pos);
            if (tileentity instanceof CommandBlockTileEntity) {
                CommandBlockTileEntity commandblocktileentity = (CommandBlockTileEntity) tileentity;
                boolean flag = worldIn.isBlockPowered(pos);
                boolean flag1 = commandblocktileentity.isPowered();

                org.bukkit.block.Block bukkitBlock = CraftBlock.at(worldIn, pos);
                int old = flag1 ? 15 : 0;
                int current = flag ? 15 : 0;
                BlockRedstoneEvent eventRedstone = new BlockRedstoneEvent(bukkitBlock, old, current);
                Bukkit.getPluginManager().callEvent(eventRedstone);
                flag = eventRedstone.getNewCurrent() > 0;

                commandblocktileentity.setPowered(flag);
                if (!flag1 && !commandblocktileentity.isAuto() && commandblocktileentity.getMode() != CommandBlockTileEntity.Mode.SEQUENCE) {
                    if (flag) {
                        commandblocktileentity.setConditionMet();
                        worldIn.getPendingBlockTicks().scheduleTick(pos, (CommandBlockBlock) (Object) this, 1);
                    }

                }
            }
        }
    }
}
