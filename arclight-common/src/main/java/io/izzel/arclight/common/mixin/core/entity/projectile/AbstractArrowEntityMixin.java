package io.izzel.arclight.common.mixin.core.entity.projectile;

import io.izzel.arclight.common.bridge.entity.EntityBridge;
import io.izzel.arclight.common.bridge.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.mixin.core.entity.EntityMixin;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.entity.CraftItem;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.player.PlayerPickupArrowEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import io.izzel.arclight.common.bridge.entity.player.PlayerInventoryBridge;

import javax.annotation.Nullable;

@Mixin(AbstractArrowEntity.class)
public abstract class AbstractArrowEntityMixin extends EntityMixin {

    // @formatter:off
    @Shadow public boolean inGround;
    @Shadow public abstract boolean getNoClip();
    @Shadow public int arrowShake;
    @Shadow public AbstractArrowEntity.PickupStatus pickupStatus;
    @Shadow @Nullable public abstract Entity getShooter();
    @Shadow protected abstract ItemStack getArrowStack();
    // @formatter:on

    @Inject(method = "onHit", at = @At("HEAD"))
    private void arclight$projectileHit(RayTraceResult raytraceResultIn, CallbackInfo ci) {
        CraftEventFactory.callProjectileHitEvent((AbstractArrowEntity) (Object) this, raytraceResultIn);
    }

    @Redirect(method = "onEntityHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;setFire(I)V"))
    private void arclight$fireShot(Entity entity, int seconds, EntityRayTraceResult result) {
        EntityCombustByEntityEvent combustEvent = new EntityCombustByEntityEvent(this.getBukkitEntity(), ((EntityBridge) entity).bridge$getBukkitEntity(), seconds);
        Bukkit.getPluginManager().callEvent(combustEvent);
        if (!combustEvent.isCancelled()) {
            ((EntityBridge) entity).bridge$setOnFire(combustEvent.getDuration(), false);
        }
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void onCollideWithPlayer(final PlayerEntity entityhuman) {
        if (!this.world.isRemote && (this.inGround || this.getNoClip()) && this.arrowShake <= 0) {
            ItemStack itemstack = this.getArrowStack();
            if (this.pickupStatus == AbstractArrowEntity.PickupStatus.ALLOWED && !itemstack.isEmpty() && ((PlayerInventoryBridge) entityhuman.inventory).bridge$canHold(itemstack) > 0) {
                final ItemEntity item = new ItemEntity(this.world, this.posX, this.posY, this.posZ, itemstack);
                final PlayerPickupArrowEvent event = new PlayerPickupArrowEvent(((ServerPlayerEntityBridge) entityhuman).bridge$getBukkitEntity(), new CraftItem(((CraftServer) Bukkit.getServer()), (AbstractArrowEntity) (Object) this, item), (AbstractArrow) this.getBukkitEntity());
                Bukkit.getPluginManager().callEvent(event);
                if (event.isCancelled()) {
                    return;
                }
                itemstack = item.getItem();
            }
            boolean flag = this.pickupStatus == AbstractArrowEntity.PickupStatus.ALLOWED || (this.pickupStatus == AbstractArrowEntity.PickupStatus.CREATIVE_ONLY && entityhuman.abilities.isCreativeMode) || (this.getNoClip() && this.getShooter().getUniqueID() == entityhuman.getUniqueID());
            if (this.pickupStatus == AbstractArrowEntity.PickupStatus.ALLOWED && !entityhuman.inventory.addItemStackToInventory(itemstack)) {
                flag = false;
            }
            if (flag) {
                entityhuman.onItemPickup((AbstractArrowEntity) (Object) this, 1);
                this.remove();
            }
        }
    }

    @Inject(method = "setShooter", at = @At("HEAD"))
    private void arclight$setShooter(Entity entityIn, CallbackInfo ci) {
        this.projectileSource = entityIn == null ? null : (ProjectileSource) ((EntityBridge) entityIn).bridge$getBukkitEntity();
    }
}
