package io.izzel.arclight.common.mixin.core.item;

import io.izzel.arclight.common.bridge.entity.player.ServerPlayerEntityBridge;
import net.minecraft.block.BlockState;
import net.minecraft.block.IBucketPickupHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.BucketItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SChangeBlockPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
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
    @Shadow public abstract boolean tryPlaceContainedLiquid(@javax.annotation.Nullable PlayerEntity player, World worldIn, BlockPos posIn, @javax.annotation.Nullable BlockRayTraceResult rayTrace);
    // @formatter:on

    @Inject(method = "onItemRightClick", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/block/IBucketPickupHandler;pickupFluid(Lnet/minecraft/world/IWorld;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Lnet/minecraft/fluid/Fluid;"))
    private void arclight$bucketFill(World worldIn, PlayerEntity playerIn, Hand handIn, CallbackInfoReturnable<ActionResult<ItemStack>> cir, ItemStack stack, RayTraceResult result) {
        BlockPos pos = ((BlockRayTraceResult) result).getPos();
        BlockState state = worldIn.getBlockState(pos);
        Fluid dummyFluid = ((IBucketPickupHandler) state.getBlock()).pickupFluid(DummyGeneratorAccess.INSTANCE, pos, state);
        PlayerBucketFillEvent event = CraftEventFactory.callPlayerBucketFillEvent((ServerWorld) worldIn, playerIn, pos, pos, ((BlockRayTraceResult) result).getFace(), stack, dummyFluid.getFilledBucket());
        if (event.isCancelled()) {
            ((ServerPlayerEntity) playerIn).connection.sendPacket(new SChangeBlockPacket(worldIn, pos));
            ((ServerPlayerEntityBridge) playerIn).bridge$getBukkitEntity().updateInventory();
            cir.setReturnValue(new ActionResult<>(ActionResultType.FAIL, stack));
        } else {
            arclight$captureItem = event.getItemStack();
        }
    }

    @Inject(method = "onItemRightClick", locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/item/BucketItem;tryPlaceContainedLiquid(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/BlockRayTraceResult;)Z"))
    private void arclight$capture(World worldIn, PlayerEntity playerIn, Hand handIn, CallbackInfoReturnable<ActionResult<ItemStack>> cir, ItemStack stack, RayTraceResult result) {
        BlockRayTraceResult blockRayTraceResult = (BlockRayTraceResult) result;
        arclight$direction = blockRayTraceResult.getFace();
        arclight$click = blockRayTraceResult.getPos();
        arclight$stack = stack;
    }

    @Inject(method = "onItemRightClick", at = @At("RETURN"))
    private void arclight$clean(World worldIn, PlayerEntity playerIn, Hand handIn, CallbackInfoReturnable<ActionResult<ItemStack>> cir) {
        arclight$captureItem = null;
        arclight$direction = null;
        arclight$click = null;
        arclight$stack = null;
    }

    private transient org.bukkit.inventory.@Nullable ItemStack arclight$captureItem;

    @ModifyArg(method = "onItemRightClick", index = 2, at = @At(value = "INVOKE", target = "Lnet/minecraft/util/DrinkHelper;fill(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/item/ItemStack;)Lnet/minecraft/item/ItemStack;"))
    private ItemStack arclight$useEventItem(ItemStack itemStack) {
        return arclight$captureItem == null ? itemStack : CraftItemStack.asNMSCopy(arclight$captureItem);
    }

    public boolean a(PlayerEntity entity, World world, BlockPos pos, @Nullable BlockRayTraceResult result, Direction direction, BlockPos clicked, ItemStack itemstack) {
        arclight$direction = direction;
        arclight$click = clicked;
        arclight$stack = itemstack;
        try {
            return this.tryPlaceContainedLiquid(entity, world, pos, result);
        } finally {
            arclight$direction = null;
            arclight$click = null;
            arclight$stack = null;
        }
    }

    private transient Direction arclight$direction;
    private transient BlockPos arclight$click;
    private transient ItemStack arclight$stack;

    @Inject(method = "tryPlaceContainedLiquid", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/DimensionType;isUltrawarm()Z"))
    private void arclight$bucketEmpty(PlayerEntity player, World worldIn, BlockPos posIn, BlockRayTraceResult rayTrace, CallbackInfoReturnable<Boolean> cir) {
        if (player != null) {
            PlayerBucketEmptyEvent event = CraftEventFactory.callPlayerBucketEmptyEvent((ServerWorld) worldIn, player, posIn, arclight$click, arclight$direction, arclight$stack);
            if (event.isCancelled()) {
                ((ServerPlayerEntity) player).connection.sendPacket(new SChangeBlockPacket(worldIn, posIn));
                ((ServerPlayerEntityBridge) player).bridge$getBukkitEntity().updateInventory();
                cir.setReturnValue(false);
            }
        }
    }
}
