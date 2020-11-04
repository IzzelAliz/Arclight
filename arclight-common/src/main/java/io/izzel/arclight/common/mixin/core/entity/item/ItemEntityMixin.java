package io.izzel.arclight.common.mixin.core.entity.item;

import io.izzel.arclight.common.bridge.entity.LivingEntityBridge;
import io.izzel.arclight.common.bridge.entity.player.PlayerEntityBridge;
import io.izzel.arclight.common.bridge.entity.player.PlayerInventoryBridge;
import io.izzel.arclight.common.bridge.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.network.datasync.EntityDataManagerBridge;
import io.izzel.arclight.common.mixin.core.entity.EntityMixin;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.stats.Stats;
import net.minecraft.util.DamageSource;
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
    @Shadow @Final private static DataParameter<ItemStack> ITEM;
    @Shadow public int pickupDelay;
    @Shadow public abstract ItemStack getItem();
    @Shadow private UUID owner;
    // @formatter:on

    @Inject(method = "func_213858_a", cancellable = true, at = @At("HEAD"))
    private static void arclight$itemMerge(ItemEntity from, ItemStack stack1, ItemEntity to, ItemStack stack2, CallbackInfo ci) {
        if (CraftEventFactory.callItemMergeEvent(to, from).isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "attackEntityFrom", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/item/ItemEntity;markVelocityChanged()V"))
    private void arclight$damageNonLiving(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (CraftEventFactory.handleNonLivingEntityDamageEvent((ItemEntity) (Object) this, source, amount)) {
            cir.setReturnValue(false);
        }
    }

    @Override
    public void burn(float amount) {
        this.attackEntityFrom(DamageSource.IN_FIRE, amount);
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void onCollideWithPlayer(final PlayerEntity entity) {
        if (!this.world.isRemote) {
            final ItemStack itemstack = this.getItem();
            final net.minecraft.item.Item item = itemstack.getItem();
            final int i = itemstack.getCount();

            int hook = net.minecraftforge.event.ForgeEventFactory.onItemPickup((ItemEntity) (Object) this, entity);
            if (hook < 0) return;
            ItemStack copy = itemstack.copy();

            final int canHold = ((PlayerInventoryBridge) entity.inventory).bridge$canHold(itemstack);
            final int remaining = i - canHold;
            if (this.pickupDelay <= 0 && (hook == 1 || canHold > 0)) {

                copy.setCount(canHold);
                net.minecraftforge.fml.hooks.BasicEventHooks.firePlayerItemPickupEvent(entity, (ItemEntity) (Object) this, copy);

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
            if (this.pickupDelay == 0 && (this.owner == null /*|| 6000 - this.age <= 200*/ || this.owner.equals(entity.getUniqueID())) && entity.inventory.addItemStackToInventory(itemstack)) {
                entity.onItemPickup((ItemEntity) (Object) this, i);
                if (itemstack.isEmpty()) {
                    this.remove();
                    itemstack.setCount(i);
                }
                entity.addStat(Stats.ITEM_PICKED_UP.get(item), i);
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
        ((EntityDataManagerBridge) this.getDataManager()).bridge$markDirty(ITEM);
    }

    @Redirect(method = "func_226531_a_", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/item/ItemEntity;setItem(Lnet/minecraft/item/ItemStack;)V"))
    private static void arclight$setNonEmpty(ItemEntity itemEntity, ItemStack stack) {
        if (!stack.isEmpty()) {
            itemEntity.setItem(stack);
        }
    }
}
