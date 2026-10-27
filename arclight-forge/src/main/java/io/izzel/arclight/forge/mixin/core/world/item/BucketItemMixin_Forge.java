package io.izzel.arclight.forge.mixin.core.world.item;

import com.llamalad7.mixinextras.sugar.Local;
import io.izzel.arclight.common.bridge.core.server.level.ServerPlayerBridge;
import io.izzel.arclight.common.bridge.core.world.level.LevelAccessorBridge;
import io.izzel.arclight.common.bridge.core.world.item.BucketItemBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(BucketItem.class)
public abstract class BucketItemMixin_Forge implements BucketItemBridge {

    @Inject(method = "use(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;", locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", remap = false, target = "Lnet/minecraft/world/item/BucketItem;emptyContents(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/BlockHitResult;Lnet/minecraft/world/item/ItemStack;)Z"), require = 1, allow = 1)
    private void arclight$capture(Level worldIn, Player playerIn, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir, ItemStack stack, BlockHitResult result) {
        arclight$setDirection(result.getDirection());
        arclight$setClick(result.getBlockPos());
        arclight$setHand(hand);
        arclight$setStack(stack);
    }

    @Inject(method = "emptyContents(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/BlockHitResult;Lnet/minecraft/world/item/ItemStack;)Z", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;isAir()Z"), require = 1, allow = 1)
    private void arclight$bucketEmpty(LivingEntity user, Level worldIn, BlockPos posIn, BlockHitResult rayTrace, ItemStack stack, CallbackInfoReturnable<Boolean> cir, @Local(ordinal = 2) boolean placeLiquid) {
        // Forge's inherited Player overload currently delegates without the stack.
        ItemStack bucket = stack == null ? arclight$getStack() : stack;
        if (placeLiquid && LevelAccessorBridge.from(worldIn) instanceof LevelAccessorBridge bridge && user instanceof ServerPlayer player && bucket != null) {
            PlayerBucketEmptyEvent event = CraftEventFactory.callPlayerBucketEmptyEvent(bridge.bridge$getMinecraftWorld(), player, posIn, arclight$getClick(), arclight$getDirection(), bucket, arclight$getHand() == null ? InteractionHand.MAIN_HAND : arclight$getHand());
            if (event.isCancelled()) {
                player.connection.send(new ClientboundBlockUpdatePacket(worldIn, posIn));
                ((ServerPlayerBridge) player).bridge$getBukkitEntity().updateInventory();
                cir.setReturnValue(false);
            } else {
                // Preserve native/modded remainders unless a plugin changed it.
                org.bukkit.inventory.ItemStack remainder = event.getItemStack();
                if (remainder != null && !remainder.equals(new org.bukkit.inventory.ItemStack(org.bukkit.Material.BUCKET))) {
                    arclight$setCaptureItem(remainder.clone());
                }
            }
        }
    }
}
