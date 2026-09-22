package io.izzel.arclight.common.mixin.core.world.item;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.izzel.arclight.common.mod.util.BucketUseContext;
import io.izzel.arclight.common.bridge.core.server.level.ServerPlayerBridge;
import io.izzel.arclight.common.bridge.core.world.level.LevelAccessorBridge;
import io.izzel.arclight.common.bridge.core.world.item.BucketItemBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v.util.DummyGeneratorAccess;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BucketItem.class)
public abstract class BucketItemMixin implements BucketItemBridge {

    // @formatter:off
    @Shadow public abstract boolean emptyContents(@Nullable LivingEntity player, Level worldIn, BlockPos posIn, @javax.annotation.Nullable BlockHitResult rayTrace);
    // @formatter:on

    // Using @Local doesn't work for Forge since they don't have MixinExtras :(
    // Use Decorate to capture
    @Decorate(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/BucketPickup;pickupBlock(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack arclight$bucketFill(BucketPickup pickup, LivingEntity entity, LevelAccessor worldIn, BlockPos pos, BlockState state,
                                          @Local(ordinal = 0) InteractionHand handIn, @Local(ordinal = 0) ItemStack stack, @Local(ordinal = 0) BlockHitResult result) throws Throwable {
        if (entity instanceof Player playerIn && LevelAccessorBridge.from(worldIn) instanceof LevelAccessorBridge bridge) {
            ItemStack dummyFluid = pickup.pickupBlock(playerIn, DummyGeneratorAccess.INSTANCE, pos, state);
            PlayerBucketFillEvent event = CraftEventFactory.callPlayerBucketFillEvent(bridge.bridge$getMinecraftWorld(), playerIn, pos, pos, result.getDirection(), stack, dummyFluid.getItem(), handIn);
            if (event.isCancelled()) {
                ((ServerPlayer) playerIn).connection.send(new ClientboundBlockUpdatePacket(worldIn, pos));
                ((ServerPlayerBridge) playerIn).bridge$getBukkitEntity().updateInventory();
                return (ItemStack) DecorationOps.cancel().invoke((InteractionResult) InteractionResult.FAIL);
            } else {
                arclight$setCaptureItem(event.getItemStack());
            }
        }
        return (ItemStack) DecorationOps.callsite().invoke(pickup, entity, worldIn, pos, state);
    }

    @WrapMethod(method = "use")
    private InteractionResult arclight$useContext(Level level, Player player, InteractionHand hand, Operation<InteractionResult> original) {
        BucketUseContext previous = arclight$context.get();
        arclight$context.set(new BucketUseContext());
        try {
            return original.call(level, player, hand);
        } finally {
            if (previous == null) arclight$context.remove();
            else arclight$context.set(previous);
        }
    }

    @ModifyArg(method = "use", index = 2, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemUtils;createFilledResult(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack arclight$useEventItem(ItemStack itemStack) {
        return arclight$getCaptureItem() == null ? itemStack : CraftItemStack.asNMSCopy(arclight$getCaptureItem());
    }

    public boolean emptyContents(Player entity, Level world, BlockPos pos, @Nullable BlockHitResult result, Direction direction, BlockPos clicked, ItemStack itemstack, InteractionHand hand) {
        BucketUseContext previous = arclight$context.get();
        arclight$context.set(new BucketUseContext());
        arclight$setDirection(direction);
        arclight$setClick(clicked);
        arclight$setHand(hand);
        arclight$setStack(itemstack);
        try {
            return this.emptyContents(entity, world, pos, result);
        } finally {
            if (previous == null) arclight$context.remove();
            else arclight$context.set(previous);
        }
    }

    @Unique
    private final ThreadLocal<BucketUseContext> arclight$context = new ThreadLocal<>();

    @Nullable
    @Override
    public Direction arclight$getDirection() {
        BucketUseContext context = this.arclight$context.get();
        return context == null ? null : context.direction;
    }

    @Override
    public void arclight$setDirection(@Nullable Direction value) {
        BucketUseContext context = this.arclight$context.get();
        if (context != null) context.direction = value;
    }

    @Nullable
    @Override
    public BlockPos arclight$getClick() {
        BucketUseContext context = this.arclight$context.get();
        return context == null ? null : context.click;
    }

    @Override
    public void arclight$setClick(@Nullable BlockPos value) {
        BucketUseContext context = this.arclight$context.get();
        if (context != null) context.click = value;
    }

    @Nullable
    @Override
    public InteractionHand arclight$getHand() {
        BucketUseContext context = this.arclight$context.get();
        return context == null ? null : context.hand;
    }

    @Override
    public void arclight$setHand(@Nullable InteractionHand value) {
        BucketUseContext context = this.arclight$context.get();
        if (context != null) context.hand = value;
    }

    @Nullable
    @Override
    public ItemStack arclight$getStack() {
        BucketUseContext context = this.arclight$context.get();
        return context == null ? null : context.stack;
    }

    @Override
    public void arclight$setStack(@Nullable ItemStack value) {
        BucketUseContext context = this.arclight$context.get();
        if (context != null) context.stack = value;
    }

    @Nullable
    @Override
    public org.bukkit.inventory.ItemStack arclight$getCaptureItem() {
        BucketUseContext context = this.arclight$context.get();
        return context == null ? null : context.captureItem;
    }

    @Override
    public void arclight$setCaptureItem(@Nullable org.bukkit.inventory.ItemStack value) {
        BucketUseContext context = this.arclight$context.get();
        if (context != null) context.captureItem = value;
    }
}
