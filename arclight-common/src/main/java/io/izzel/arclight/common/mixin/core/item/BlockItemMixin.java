package io.izzel.arclight.common.mixin.core.item;

import io.izzel.arclight.common.bridge.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.item.BlockItemBridge;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.state.IProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.util.math.shapes.ISelectionContext;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.block.data.CraftBlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockCanBuildEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BlockItem.class)
public abstract class BlockItemMixin implements BlockItemBridge {

    // @formatter:off
    @Shadow protected abstract boolean checkPosition();
    @Shadow  private static <T extends Comparable<T>> BlockState func_219988_a(BlockState p_219988_0_, IProperty<T> p_219988_1_, String p_219988_2_) { return null; }
    // @formatter:on

    private static BlockState getBlockState(BlockState blockState, CompoundNBT nbt) {
        StateContainer<Block, BlockState> statecontainer = blockState.getBlock().getStateContainer();
        for (String s : nbt.keySet()) {
            IProperty<?> iproperty = statecontainer.getProperty(s);
            if (iproperty != null) {
                String s1 = nbt.get(s).getString();
                blockState = func_219988_a(blockState, iproperty, s1);
            }
        }
        return blockState;
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    protected boolean canPlace(BlockItemUseContext context, BlockState state) {
        PlayerEntity playerentity = context.getPlayer();
        ISelectionContext iselectioncontext = playerentity == null ? ISelectionContext.dummy() : ISelectionContext.forEntity(playerentity);
        boolean original = (!this.checkPosition() || state.isValidPosition(context.getWorld(), context.getPos())) && this.bridge$noCollisionInSel(context.getWorld(), state, context.getPos(), iselectioncontext);

        Player player = (context.getPlayer() instanceof ServerPlayerEntityBridge) ? ((ServerPlayerEntityBridge) context.getPlayer()).bridge$getBukkitEntity() : null;
        BlockCanBuildEvent event = new BlockCanBuildEvent(CraftBlock.at(context.getWorld(), context.getPos()), player, CraftBlockData.fromData(state), original);
        Bukkit.getPluginManager().callEvent(event);
        return event.isBuildable();
    }
}
