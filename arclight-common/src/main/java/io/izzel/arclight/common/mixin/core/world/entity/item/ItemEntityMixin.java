package io.izzel.arclight.common.mixin.core.world.entity.item;

import io.izzel.arclight.common.bridge.core.entity.LivingEntityBridge;
import io.izzel.arclight.common.bridge.core.entity.player.PlayerEntityBridge;
import io.izzel.arclight.common.bridge.core.entity.player.PlayerInventoryBridge;
import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.core.network.datasync.EntityDataManagerBridge;
import io.izzel.arclight.common.bridge.core.world.WorldBridge;
import io.izzel.arclight.common.mixin.core.world.entity.EntityMixin;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.stats.Stats;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.ForgeEventFactory;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.entity.Item;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin extends EntityMixin {

    // @formatter:off
    @Shadow @Final private static EntityDataAccessor<ItemStack> DATA_ITEM;
    @Shadow public int pickupDelay;
    @Shadow public abstract ItemStack getItem();
    @Shadow private UUID owner;
    // @formatter:on

    @Inject(method = "merge(Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/world/item/ItemStack;)V", cancellable = true, at = @At("HEAD"))
    private static void arclight$itemMerge(ItemEntity from, ItemStack stack1, ItemEntity to, ItemStack stack2, CallbackInfo ci) {
        if (CraftEventFactory.callItemMergeEvent(to, from).isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "hurt", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/ItemEntity;markHurt()V"))
    private void arclight$damageNonLiving(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (CraftEventFactory.handleNonLivingEntityDamageEvent((ItemEntity) (Object) this, source, amount)) {
            cir.setReturnValue(false);
        }
    }

    @Override
    public void burn(float amount) {
        this.hurt(DamageSource.IN_FIRE, amount);
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void playerTouch(final Player entity) {
        if (!this.level.isClientSide) {
            if (this.pickupDelay > 0) return;
            final ItemStack itemstack = this.getItem();
            final net.minecraft.world.item.Item item = itemstack.getItem();
            final int i = itemstack.getCount();

            int hook = net.minecraftforge.event.ForgeEventFactory.onItemPickup((ItemEntity) (Object) this, entity);
            if (hook < 0) return;
            ItemStack copy = itemstack.copy();

            final int canHold = ((PlayerInventoryBridge) entity.getInventory()).bridge$canHold(itemstack);
            final int remaining = i - canHold;
            if (this.pickupDelay <= 0 && (hook == 1 || canHold > 0)) {

                copy.setCount(canHold);
                ForgeEventFactory.firePlayerItemPickupEvent(entity, (ItemEntity) (Object) this, copy);

                itemstack.setCount(canHold);
                final PlayerPickupItemEvent playerEvent = new PlayerPickupItemEvent(((ServerPlayerEntityBridge) entity).bridge$getBukkitEntity(), (Item) this.getBukkitEntity(), remaining);
                playerEvent.setCancelled(!((PlayerEntityBridge) entity).bridge$canPickUpLoot());
                Bukkit.getPluginManager().callEvent(playerEvent);
                if (playerEvent.isCancelled()) {
                    itemstack.setCount(i);
                    return;
                }
                final EntityPickupItemEvent entityEvent = new EntityPickupItemEvent(((LivingEntityBridge) entity).bridge$getBukkitEntity(), (Item) this.getBukkitEntity(), remaining);
                entityEvent.setCancelled(!((PlayerEntityBridge) entity).bridge$canPickUpLoot());
                Bukkit.getPluginManager().callEvent(entityEvent);
                if (entityEvent.isCancelled()) {
                    itemstack.setCount(i);
                    return;
                }
                itemstack.setCount(canHold + remaining);
                this.pickupDelay = 0;
            } else if (this.pickupDelay == 0) {
                this.pickupDelay = -1;
            }
            if (this.pickupDelay == 0 && (this.owner == null /*|| 6000 - this.age <= 200*/ || this.owner.equals(entity.getUUID())) && entity.getInventory().add(itemstack)) {
                entity.take((ItemEntity) (Object) this, i);
                if (itemstack.isEmpty()) {
                    this.discard();
                    itemstack.setCount(i);
                }
                entity.awardStat(Stats.ITEM_PICKED_UP.get(item), i);
                entity.onItemPickup((ItemEntity) (Object) this);
            }
        }
    }

    /* #24
    @Inject(method = "setItem", at = @At("HEAD"))
    private void arclight$noAirDrops(ItemStack stack, CallbackInfo ci) {
        Preconditions.checkArgument(!stack.isEmpty(), "Cannot drop air");
    }
    */

    @Inject(method = "setItem", at = @At("RETURN"))
    private void arclight$markDirty(ItemStack stack, CallbackInfo ci) {
        ((EntityDataManagerBridge) this.getEntityData()).bridge$markDirty(DATA_ITEM);
    }

    @Redirect(method = "merge(Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/ItemEntity;setItem(Lnet/minecraft/world/item/ItemStack;)V"))
    private static void arclight$setNonEmpty(ItemEntity itemEntity, ItemStack stack) {
        if (!stack.isEmpty()) {
            itemEntity.setItem(stack);
        }
    }

    @Redirect(method = "mergeWithNeighbours", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/AABB;inflate(DDD)Lnet/minecraft/world/phys/AABB;"))
    private AABB arclight$mergeRadius(AABB instance, double pX, double pY, double pZ) {
        double radius = ((WorldBridge) level).bridge$spigotConfig().itemMerge;
        return instance.inflate(radius);
    }
}
