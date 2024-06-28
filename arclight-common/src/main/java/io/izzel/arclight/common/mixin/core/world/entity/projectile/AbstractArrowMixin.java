package io.izzel.arclight.common.mixin.core.world.entity.projectile;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.entity.player.PlayerInventoryBridge;
import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.entity.CraftItem;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.player.PlayerPickupArrowEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.world.entity.projectile.AbstractArrow.class)
public abstract class AbstractArrowMixin extends ProjectileMixin {

    // @formatter:off
    @Shadow public boolean inGround;
    @Shadow public abstract boolean isNoPhysics();
    @Shadow public int shakeTime;
    @Shadow public net.minecraft.world.entity.projectile.AbstractArrow.Pickup pickup;
    @Shadow protected abstract ItemStack getPickupItem();
    @Shadow public ItemStack pickupItemStack;
    // @formatter:on

    @Decorate(method = "onHitEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;igniteForSeconds(F)V"))
    private void arclight$fireShot(Entity entity, float seconds) throws Throwable {
        EntityCombustByEntityEvent combustEvent = new EntityCombustByEntityEvent(this.getBukkitEntity(), entity.bridge$getBukkitEntity(), seconds);
        Bukkit.getPluginManager().callEvent(combustEvent);
        if (!combustEvent.isCancelled()) {
            DecorationOps.callsite().invoke(entity, seconds);
        }
    }

    @Inject(method = "onHitEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/AbstractArrow;discard()V"))
    private void arclight$hit(CallbackInfo ci) {
        this.bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.HIT);
    }

    @Inject(method = "tickDespawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/AbstractArrow;discard()V"))
    private void arclight$despawn(CallbackInfo ci) {
        this.bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.DESPAWN);
    }

    @Decorate(method = "playerTouch", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/AbstractArrow;tryPickup(Lnet/minecraft/world/entity/player/Player;)Z"))
    private boolean arclight$pickupArrow(net.minecraft.world.entity.projectile.AbstractArrow instance, Player player) throws Throwable {
        ItemStack itemstack = this.getPickupItem();
        if (this.pickup == net.minecraft.world.entity.projectile.AbstractArrow.Pickup.ALLOWED && !itemstack.isEmpty() && ((PlayerInventoryBridge) player.getInventory()).bridge$canHold(itemstack) > 0) {
            ItemEntity item = new ItemEntity(this.level(), this.getX(), this.getY(), this.getZ(), itemstack);
            PlayerPickupArrowEvent event = new PlayerPickupArrowEvent(((ServerPlayerEntityBridge) player).bridge$getBukkitEntity(), new CraftItem(((CraftServer) Bukkit.getServer()), item), (AbstractArrow) this.getBukkitEntity());
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return (boolean) DecorationOps.cancel().invoke();
            }
            itemstack = item.getItem();
        }
        this.pickupItemStack = itemstack;
        var result = (boolean) DecorationOps.callsite().invoke(instance, player);
        if (result) {
            this.bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.PICKUP);
        }
        return result;
    }

    @Inject(method = "setOwner", at = @At("HEAD"))
    private void arclight$setShooter(Entity entityIn, CallbackInfo ci) {
        this.projectileSource = entityIn == null ? null : (ProjectileSource) ((EntityBridge) entityIn).bridge$getBukkitEntity();
    }
}
