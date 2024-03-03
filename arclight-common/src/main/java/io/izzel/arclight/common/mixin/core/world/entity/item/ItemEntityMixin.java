package io.izzel.arclight.common.mixin.core.world.entity.item;

import io.izzel.arclight.common.bridge.core.entity.LivingEntityBridge;
import io.izzel.arclight.common.bridge.core.entity.item.ItemEntityBridge;
import io.izzel.arclight.common.bridge.core.entity.player.PlayerEntityBridge;
import io.izzel.arclight.common.bridge.core.entity.player.PlayerInventoryBridge;
import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.core.network.datasync.SynchedEntityDataBridge;
import io.izzel.arclight.common.bridge.core.world.WorldBridge;
import io.izzel.arclight.common.mixin.core.world.entity.EntityMixin;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.stats.Stats;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.entity.Item;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin extends EntityMixin implements ItemEntityBridge {

    // @formatter:off
    @Shadow @Final private static EntityDataAccessor<ItemStack> DATA_ITEM;
    @Shadow public int pickupDelay;
    @Shadow public abstract ItemStack getItem();
    @Shadow public UUID target;
    @Shadow public int age;
    // @formatter:on

    @Inject(method = "merge(Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/world/item/ItemStack;)V", cancellable = true, at = @At("HEAD"))
    private static void arclight$itemMerge(ItemEntity from, ItemStack stack1, ItemEntity to, ItemStack stack2, CallbackInfo ci) {
        if (!CraftEventFactory.callItemMergeEvent(to, from)) {
            ci.cancel();
        }
    }

    @Inject(method = "merge(Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/world/item/ItemStack;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/ItemEntity;discard()V"))
    private static void arclight$itemMergeCause(ItemEntity from, ItemStack stack1, ItemEntity to, ItemStack stack2, CallbackInfo ci) {
        to.bridge().bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.MERGE);
    }

    @Inject(method = "hurt", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/ItemEntity;markHurt()V"))
    private void arclight$damageNonLiving(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (CraftEventFactory.handleNonLivingEntityDamageEvent((ItemEntity) (Object) this, source, amount)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "hurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/ItemEntity;discard()V"))
    private void arclight$dead(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        this.bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.DEATH);
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void playerTouch(final Player entity) {
        if (!this.level().isClientSide) {
            if (this.pickupDelay > 0) return;
            ItemStack itemstack = this.getItem();
            int i = itemstack.getCount();

            int hook = this.bridge$forge$onItemPickup(entity);
            if (hook < 0) return;

            final int canHold = ((PlayerInventoryBridge) entity.getInventory()).bridge$canHold(itemstack);
            final int remaining = itemstack.getCount() - canHold;
            if (this.pickupDelay <= 0 && canHold > 0) {
                itemstack.setCount(canHold);
                final PlayerPickupItemEvent playerEvent = new PlayerPickupItemEvent(((ServerPlayerEntityBridge) entity).bridge$getBukkitEntity(), (Item) this.getBukkitEntity(), remaining);
                playerEvent.setCancelled(!((PlayerEntityBridge) entity).bridge$canPickUpLoot());
                Bukkit.getPluginManager().callEvent(playerEvent);
                if (playerEvent.isCancelled()) {
                    itemstack.setCount(canHold + remaining);
                    return;
                }
                final EntityPickupItemEvent entityEvent = new EntityPickupItemEvent(((LivingEntityBridge) entity).bridge$getBukkitEntity(), (Item) this.getBukkitEntity(), remaining);
                entityEvent.setCancelled(!((PlayerEntityBridge) entity).bridge$canPickUpLoot());
                Bukkit.getPluginManager().callEvent(entityEvent);
                if (entityEvent.isCancelled()) {
                    itemstack.setCount(canHold + remaining);
                    return;
                }
                ItemStack current = this.getItem();
                if (!itemstack.equals(current)) {
                    itemstack = current;
                } else {
                    itemstack.setCount(canHold + remaining);
                }
                this.pickupDelay = 0;
            } else if (this.pickupDelay == 0 && hook != 1) {
                this.pickupDelay = -1;
            }
            ItemStack copy = itemstack.copy();
            if (this.pickupDelay == 0 && (this.target == null /*|| 6000 - this.age <= 200*/ || this.target.equals(entity.getUUID())) && (hook == 1 || entity.getInventory().add(itemstack))) {
                copy.setCount(copy.getCount() - itemstack.getCount());
                this.bridge$forge$firePlayerItemPickupEvent(entity, copy);
                entity.take((ItemEntity) (Object) this, i);
                if (itemstack.isEmpty()) {
                    this.bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.PICKUP);
                    this.discard();
                    itemstack.setCount(i);
                }
                entity.awardStat(Stats.ITEM_PICKED_UP.get(itemstack.getItem()), i);
                entity.onItemPickup((ItemEntity) (Object) this);
            }
        }
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/ItemEntity;discard()V"),
        slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/item/ItemEntity;age:I")))
    private void arclight$itemDespawn(ItemEntity instance) {
        if (this.bridge$common$itemDespawnEvent()) {
            instance.discard();
        }
    }

    @Override
    public boolean bridge$common$itemDespawnEvent() {
        if (CraftEventFactory.callItemDespawnEvent((ItemEntity) (Object) this).isCancelled()) {
            this.age = 0;
            return false;
        }
        return true;
    }

    @Inject(method = "setItem", at = @At("RETURN"))
    private void arclight$markDirty(ItemStack stack, CallbackInfo ci) {
        ((SynchedEntityDataBridge) this.getEntityData()).bridge$markDirty(DATA_ITEM);
    }

    @Redirect(method = "mergeWithNeighbours", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/AABB;inflate(DDD)Lnet/minecraft/world/phys/AABB;"))
    private AABB arclight$mergeRadius(AABB instance, double pX, double pY, double pZ) {
        double radius = ((WorldBridge) level()).bridge$spigotConfig().itemMerge;
        return instance.inflate(radius);
    }
}
