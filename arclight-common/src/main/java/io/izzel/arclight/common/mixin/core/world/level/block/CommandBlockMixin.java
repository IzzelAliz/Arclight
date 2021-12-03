package io.izzel.arclight.common.mixin.core.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CommandBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(CommandBlock.class)
public abstract class CommandBlockMixin {

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void neighborChanged(BlockState state, Level worldIn, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving) {
        if (!worldIn.isClientSide) {
            BlockEntity tileentity = worldIn.getBlockEntity(pos);
            if (tileentity instanceof CommandBlockEntity commandblocktileentity) {
                boolean flag = worldIn.hasNeighborSignal(pos);
                boolean flag1 = commandblocktileentity.isPowered();

                org.bukkit.block.Block bukkitBlock = CraftBlock.at(worldIn, pos);
                int old = flag1 ? 15 : 0;
                int current = flag ? 15 : 0;
                BlockRedstoneEvent eventRedstone = new BlockRedstoneEvent(bukkitBlock, old, current);
                Bukkit.getPluginManager().callEvent(eventRedstone);
                flag = eventRedstone.getNewCurrent() > 0;

                commandblocktileentity.setPowered(flag);
                if (!flag1 && !commandblocktileentity.isAutomatic() && commandblocktileentity.getMode() != CommandBlockEntity.Mode.SEQUENCE) {
                    if (flag) {
                        commandblocktileentity.markConditionMet();
                        worldIn.scheduleTick(pos, (CommandBlock) (Object) this, 1);
                    }

                }
            }
        }
    }
}
