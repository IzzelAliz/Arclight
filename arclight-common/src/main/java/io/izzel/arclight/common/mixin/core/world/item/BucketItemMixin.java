package io.izzel.arclight.common.mixin.core.world.item;

import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.mod.util.DistValidate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v.util.DummyGeneratorAccess;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(BucketItem.class)
public abstract class BucketItemMixin {

    // @formatter:off
    @Shadow(remap = false) public abstract boolean emptyContents(@javax.annotation.Nullable Player player, Level worldIn, BlockPos posIn, @javax.annotation.Nullable BlockHitResult rayTrace, ItemStack stack);
    // @formatter:on

    @Inject(method = "use", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/BucketPickup;pickupBlock(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/item/ItemStack;"))
    private void arclight$bucketFill(Level worldIn, Player playerIn, InteractionHand handIn, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir, ItemStack stack, BlockHitResult result) {
        if (!DistValidate.isValid(worldIn)) return;
        BlockPos pos = result.getBlockPos();
        BlockState state = worldIn.getBlockState(pos);
        ItemStack dummyFluid = ((BucketPickup) state.getBlock()).pickupBlock(DummyGeneratorAccess.INSTANCE, pos, state);
        PlayerBucketFillEvent event = CraftEventFactory.callPlayerBucketFillEvent((ServerLevel) worldIn, playerIn, pos, pos, result.getDirection(), stack, dummyFluid.getItem(), handIn);
        if (event.isCancelled()) {
            ((ServerPlayer) playerIn).connection.send(new ClientboundBlockUpdatePacket(worldIn, pos));
            ((ServerPlayerEntityBridge) playerIn).bridge$getBukkitEntity().updateInventory();
            cir.setReturnValue(new InteractionResultHolder<>(InteractionResult.FAIL, stack));
        } else {
            arclight$captureItem = event.getItemStack();
        }
    }

    @Inject(method = "use", locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", remap = false, target = "Lnet/minecraft/world/item/BucketItem;emptyContents(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/BlockHitResult;Lnet/minecraft/world/item/ItemStack;)Z"))
    private void arclight$capture(Level worldIn, Player playerIn, InteractionHand handIn, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir, ItemStack stack, BlockHitResult result) {
        arclight$direction = result.getDirection();
        arclight$click = result.getBlockPos();
        arclight$hand = handIn;
    }

    @Inject(method = "use", at = @At("RETURN"))
    private void arclight$clean(Level worldIn, Player playerIn, InteractionHand handIn, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        arclight$captureItem = null;
        arclight$direction = null;
        arclight$click = null;
    }

    private transient org.bukkit.inventory.@Nullable ItemStack arclight$captureItem;

    @ModifyArg(method = "use", index = 2, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemUtils;createFilledResult(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack arclight$useEventItem(ItemStack itemStack) {
        return arclight$captureItem == null ? itemStack : CraftItemStack.asNMSCopy(arclight$captureItem);
    }

    public boolean emptyContents(Player entity, Level world, BlockPos pos, @Nullable BlockHitResult result, Direction direction, BlockPos clicked, ItemStack itemstack, InteractionHand hand) {
        arclight$direction = direction;
        arclight$click = clicked;
        arclight$hand = hand;
        try {
            return this.emptyContents(entity, world, pos, result, itemstack);
        } finally {
            arclight$direction = null;
            arclight$click = null;
        }
    }

    private transient Direction arclight$direction;
    private transient BlockPos arclight$click;
    private transient InteractionHand arclight$hand;

    @Inject(method = "emptyContents(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/BlockHitResult;Lnet/minecraft/world/item/ItemStack;)Z", remap = false, cancellable = true, at = @At(value = "INVOKE", remap = true, target = "Lnet/minecraft/world/level/dimension/DimensionType;ultraWarm()Z"))
    private void arclight$bucketEmpty(Player player, Level worldIn, BlockPos posIn, BlockHitResult rayTrace, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (!DistValidate.isValid(worldIn)) return;
        if (player != null && stack != null) {
            PlayerBucketEmptyEvent event = CraftEventFactory.callPlayerBucketEmptyEvent((ServerLevel) worldIn, player, posIn, arclight$click, arclight$direction, stack, arclight$hand == null ? InteractionHand.MAIN_HAND : arclight$hand);
            if (event.isCancelled()) {
                ((ServerPlayer) player).connection.send(new ClientboundBlockUpdatePacket(worldIn, posIn));
                ((ServerPlayerEntityBridge) player).bridge$getBukkitEntity().updateInventory();
                cir.setReturnValue(false);
            }
        }
    }
}
