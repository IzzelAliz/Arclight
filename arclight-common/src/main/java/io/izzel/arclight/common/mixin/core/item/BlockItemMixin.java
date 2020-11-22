package io.izzel.arclight.common.mixin.core.item;

import io.izzel.arclight.common.bridge.entity.player.ServerPlayerEntityBridge;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.LilyPadItem;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.state.Property;
import net.minecraft.state.StateContainer;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.world.server.ServerWorld;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.block.CraftBlockState;
import org.bukkit.craftbukkit.v.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockCanBuildEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(BlockItem.class)
public abstract class BlockItemMixin {

    // @formatter:off
    @Shadow protected abstract boolean checkPosition();
    @Shadow private static <T extends Comparable<T>> BlockState func_219988_a(BlockState p_219988_0_, Property<T> p_219988_1_, String p_219988_2_) { return null; }
    // @formatter:on

    private transient org.bukkit.block.BlockState arclight$state;

    @Inject(method = "tryPlace", locals = LocalCapture.CAPTURE_FAILHARD,
        at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/item/BlockItem;getStateForPlacement(Lnet/minecraft/item/BlockItemUseContext;)Lnet/minecraft/block/BlockState;"))
    private void arclight$prePlaceLilypad(BlockItemUseContext context, CallbackInfoReturnable<ActionResultType> cir, BlockItemUseContext context1) {
        if ((Object) this instanceof LilyPadItem) {
            this.arclight$state = CraftBlockState.getBlockState(context1.getWorld(), context1.getPos());
        }
    }

    @Inject(method = "tryPlace", locals = LocalCapture.CAPTURE_FAILHARD, cancellable = true,
        at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/block/Block;onBlockPlacedBy(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;)V"))
    private void arclight$postPlaceLilypad(BlockItemUseContext context, CallbackInfoReturnable<ActionResultType> cir, BlockItemUseContext context1) {
        org.bukkit.block.BlockState state = arclight$state;
        arclight$state = null;
        BlockPos pos = context1.getPos();
        if (state != null) {
            org.bukkit.event.block.BlockPlaceEvent placeEvent = CraftEventFactory.callBlockPlaceEvent((ServerWorld) context1.getWorld(), context1.getPlayer(), context1.getHand(), state, pos.getX(), pos.getY(), pos.getZ());
            if (placeEvent != null && (placeEvent.isCancelled() || !placeEvent.canBuild())) {
                state.update(true, false);
                cir.setReturnValue(ActionResultType.FAIL);
            }
        }
    }

    @Inject(method = "tryPlace", at = @At("RETURN"))
    private void arclight$cleanup(BlockItemUseContext context, CallbackInfoReturnable<ActionResultType> cir) {
        this.arclight$state = null;
    }

    private static BlockState getBlockState(BlockState blockState, CompoundNBT nbt) {
        StateContainer<Block, BlockState> statecontainer = blockState.getBlock().getStateContainer();
        for (String s : nbt.keySet()) {
            Property<?> iproperty = statecontainer.getProperty(s);
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
        boolean original = (!this.checkPosition() || state.isValidPosition(context.getWorld(), context.getPos())) && context.getWorld().placedBlockCollides(state, context.getPos(), iselectioncontext);

        Player player = (context.getPlayer() instanceof ServerPlayerEntityBridge) ? ((ServerPlayerEntityBridge) context.getPlayer()).bridge$getBukkitEntity() : null;
        BlockCanBuildEvent event = new BlockCanBuildEvent(CraftBlock.at(context.getWorld(), context.getPos()), player, CraftBlockData.fromData(state), original);
        Bukkit.getPluginManager().callEvent(event);
        return event.isBuildable();
    }
}
