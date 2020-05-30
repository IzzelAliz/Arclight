package io.izzel.arclight.common.mixin.core.item;

import net.minecraft.block.AbstractRailBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.item.minecart.AbstractMinecartEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.item.MinecartItem;
import net.minecraft.state.properties.RailShape;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(MinecartItem.class)
public class MinecartItemMixin {

    // @formatter:off
    @Shadow @Final private AbstractMinecartEntity.Type minecartType;
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public ActionResultType onItemUse(ItemUseContext context) {
        World world = context.getWorld();
        BlockPos blockpos = context.getPos();
        BlockState blockstate = world.getBlockState(blockpos);
        if (!blockstate.isIn(BlockTags.RAILS)) {
            return ActionResultType.FAIL;
        } else {
            ItemStack itemstack = context.getItem();
            if (!world.isRemote) {
                RailShape railshape = blockstate.getBlock() instanceof AbstractRailBlock ? ((AbstractRailBlock) blockstate.getBlock()).getRailDirection(blockstate, world, blockpos, null) : RailShape.NORTH_SOUTH;
                double d0 = 0.0D;
                if (railshape.isAscending()) {
                    d0 = 0.5D;
                }

                AbstractMinecartEntity abstractminecartentity = AbstractMinecartEntity.create(world, (double) blockpos.getX() + 0.5D, (double) blockpos.getY() + 0.0625D + d0, (double) blockpos.getZ() + 0.5D, this.minecartType);
                if (itemstack.hasDisplayName()) {
                    abstractminecartentity.setCustomName(itemstack.getDisplayName());
                }
                if (CraftEventFactory.callEntityPlaceEvent(context, abstractminecartentity).isCancelled()) {
                    return ActionResultType.FAIL;
                }
                if (!world.addEntity(abstractminecartentity)) {
                    return ActionResultType.PASS;
                }
            }

            itemstack.shrink(1);
            return ActionResultType.SUCCESS;
        }
    }
}
