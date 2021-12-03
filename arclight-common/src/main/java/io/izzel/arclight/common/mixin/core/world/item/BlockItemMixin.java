package io.izzel.arclight.common.mixin.core.world.item;

import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.mod.util.DistValidate;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.WaterLilyBlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.block.CraftBlockStates;
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
    @Shadow protected abstract boolean mustSurvive();
    @Shadow private static <T extends Comparable<T>> BlockState updateState(BlockState p_219988_0_, Property<T> p_219988_1_, String p_219988_2_) { return null; }
    // @formatter:on

    private transient org.bukkit.block.BlockState arclight$state;

    @Inject(method = "place", locals = LocalCapture.CAPTURE_FAILHARD,
        at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/world/item/BlockItem;getPlacementState(Lnet/minecraft/world/item/context/BlockPlaceContext;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private void arclight$prePlaceLilypad(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir, BlockPlaceContext context1) {
        if ((Object) this instanceof WaterLilyBlockItem) {
            this.arclight$state = CraftBlockStates.getBlockState(context1.getLevel(), context1.getClickedPos());
        }
    }

    @Inject(method = "place", locals = LocalCapture.CAPTURE_FAILHARD, cancellable = true,
        at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/world/level/block/Block;setPlacedBy(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;)V"))
    private void arclight$postPlaceLilypad(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir, BlockPlaceContext context1) {
        org.bukkit.block.BlockState state = arclight$state;
        arclight$state = null;
        BlockPos pos = context1.getClickedPos();
        if (state != null && DistValidate.isValid(context)) {
            org.bukkit.event.block.BlockPlaceEvent placeEvent = CraftEventFactory.callBlockPlaceEvent((ServerLevel) context1.getLevel(), context1.getPlayer(), context1.getHand(), state, pos.getX(), pos.getY(), pos.getZ());
            if (placeEvent != null && (placeEvent.isCancelled() || !placeEvent.canBuild())) {
                state.update(true, false);
                cir.setReturnValue(InteractionResult.FAIL);
            }
        }
    }

    @Inject(method = "place", at = @At("RETURN"))
    private void arclight$cleanup(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
        this.arclight$state = null;
    }

    private static BlockState getBlockState(BlockState blockState, CompoundTag nbt) {
        StateDefinition<Block, BlockState> statecontainer = blockState.getBlock().getStateDefinition();
        for (String s : nbt.getAllKeys()) {
            Property<?> iproperty = statecontainer.getProperty(s);
            if (iproperty != null) {
                String s1 = nbt.get(s).getAsString();
                blockState = updateState(blockState, iproperty, s1);
            }
        }
        return blockState;
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    protected boolean canPlace(BlockPlaceContext context, BlockState state) {
        net.minecraft.world.entity.player.Player playerentity = context.getPlayer();
        CollisionContext iselectioncontext = playerentity == null ? CollisionContext.empty() : CollisionContext.of(playerentity);
        boolean original = (!this.mustSurvive() || state.canSurvive(context.getLevel(), context.getClickedPos())) && context.getLevel().isUnobstructed(state, context.getClickedPos(), iselectioncontext);

        Player player = (context.getPlayer() instanceof ServerPlayerEntityBridge) ? ((ServerPlayerEntityBridge) context.getPlayer()).bridge$getBukkitEntity() : null;
        BlockCanBuildEvent event = new BlockCanBuildEvent(CraftBlock.at(context.getLevel(), context.getClickedPos()), player, CraftBlockData.fromData(state), original);
        if (DistValidate.isValid(context)) Bukkit.getPluginManager().callEvent(event);
        return event.isBuildable();
    }
}
