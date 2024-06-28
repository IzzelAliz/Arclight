package io.izzel.arclight.common.mixin.core.world.item;

import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.mod.util.DistValidate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.PlaceOnWaterBlockItem;
import net.minecraft.world.item.SolidBucketItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.block.CraftBlockStates;
import org.bukkit.craftbukkit.v.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockCanBuildEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(BlockItem.class)
public abstract class BlockItemMixin {

    // @formatter:off
    @Shadow protected abstract boolean mustSurvive();
    // @formatter:on

    private transient org.bukkit.block.BlockState arclight$state;

    @Inject(method = "place", locals = LocalCapture.CAPTURE_FAILHARD,
        at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/world/item/BlockItem;getPlacementState(Lnet/minecraft/world/item/context/BlockPlaceContext;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private void arclight$prePlaceLilypad(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir, BlockPlaceContext context1) {
        if ((Object) this instanceof PlaceOnWaterBlockItem || (Object) this instanceof SolidBucketItem) {
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
                if ((Object) this instanceof SolidBucketItem) {
                    ((ServerPlayerEntityBridge) context1.getPlayer()).bridge$getBukkitEntity().updateInventory();
                }
                cir.setReturnValue(InteractionResult.FAIL);
            }
        }
    }

    @Inject(method = "place", at = @At("RETURN"))
    private void arclight$cleanup(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
        this.arclight$state = null;
    }

    @Inject(method = "canPlace", cancellable = true, at = @At("RETURN"))
    private void arclight$blockCanBuild(BlockPlaceContext context, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        Player player = (context.getPlayer() instanceof ServerPlayerEntityBridge) ? ((ServerPlayerEntityBridge) context.getPlayer()).bridge$getBukkitEntity() : null;
        BlockCanBuildEvent event = new BlockCanBuildEvent(CraftBlock.at(context.getLevel(), context.getClickedPos()), player, CraftBlockData.fromData(state), cir.getReturnValue());
        if (DistValidate.isValid(context)) Bukkit.getPluginManager().callEvent(event);
        cir.setReturnValue(event.isBuildable());
    }
}
