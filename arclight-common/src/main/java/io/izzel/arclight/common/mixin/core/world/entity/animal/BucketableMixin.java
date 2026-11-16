package io.izzel.arclight.common.mixin.core.world.entity.animal;

import io.izzel.arclight.common.bridge.core.network.syncher.SynchedEntityDataBridge;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Bucketable;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.Level;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.player.PlayerBucketEntityEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.Optional;

@Mixin(Bucketable.class)
public interface BucketableMixin {

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    static <T extends LivingEntity & Bucketable> Optional<InteractionResult> bucketMobPickup(Player player, InteractionHand hand, LivingEntity livingEntity) {
        @SuppressWarnings("unchecked")
        T entity = (T) livingEntity;
        ItemStack itemstack = player.getItemInHand(hand);
        if (entity.canBePickedUpWithBucket(itemstack) && entity.isAlive()) {
            // entity.playSound(entity.getPickupSound(), 1.0F, 1.0F);
            ItemStack itemstack1 = entity.getBucketItemStack();
            entity.saveToBucketTag(itemstack1);
            PlayerBucketEntityEvent event = CraftEventFactory.callPlayerFishBucketEvent(entity, player, itemstack, itemstack1, hand);
            itemstack1 = CraftItemStack.asNMSCopy(event.getEntityBucket());
            if (event.isCancelled()) {
                player.containerMenu.sendAllDataToRemote(); // We need to update inventory to resync client's bucket
                entity.bridge$getBukkitEntity().update((ServerPlayer) player); // We need to play out these packets as the client assumes the fish is gone
                ((SynchedEntityDataBridge) livingEntity.getEntityData()).bridge$refresh((ServerPlayer) player); // Need to send data such as the display name to client
                return Optional.of(InteractionResult.FAIL);
            }
            entity.playSound(entity.getPickupSound(), 1.0F, 1.0F);
            ItemStack itemstack2 = ItemUtils.createFilledResult(itemstack, player, itemstack1, false);
            player.setItemInHand(hand, itemstack2);
            Level level = entity.level();
            if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                CriteriaTriggers.FILLED_BUCKET.trigger(serverPlayer, itemstack1);
            }

            if (entity instanceof Leashable leashable) {
                leashable.dropLeash();
            }

            entity.bridge().bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.PICKUP);
            entity.discard();
            return Optional.of(InteractionResult.SUCCESS);
        } else {
            return Optional.empty();
        }
    }
}
