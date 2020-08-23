package io.izzel.arclight.common.mixin.core.entity.projectile;

import io.izzel.arclight.common.bridge.entity.EntityBridge;
import io.izzel.arclight.common.bridge.entity.player.PlayerInventoryBridge;
import io.izzel.arclight.common.bridge.entity.player.ServerPlayerEntityBridge;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.EntityRayTraceResult;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.entity.CraftItem;
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

@Mixin(AbstractArrowEntity.class)
public abstract class AbstractArrowEntityMixin extends ProjectileEntityMixin {

    // @formatter:off
    @Shadow public boolean inGround;
    @Shadow public abstract boolean getNoClip();
    @Shadow public int arrowShake;
    @Shadow public AbstractArrowEntity.PickupStatus pickupStatus;
    @Shadow protected abstract ItemStack getArrowStack();
    // @formatter:on

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
    public void onCollideWithPlayer(PlayerEntity playerEntity) {
        if (!this.world.isRemote && (this.inGround || this.getNoClip()) && this.arrowShake <= 0) {
            ItemStack itemstack = this.getArrowStack();
            if (this.pickupStatus == AbstractArrowEntity.PickupStatus.ALLOWED && !itemstack.isEmpty() && ((PlayerInventoryBridge) playerEntity.inventory).bridge$canHold(itemstack) > 0) {
                ItemEntity item = new ItemEntity(this.world, this.getPosX(), this.getPosY(), this.getPosZ(), itemstack);
                PlayerPickupArrowEvent event = new PlayerPickupArrowEvent(((ServerPlayerEntityBridge) playerEntity).bridge$getBukkitEntity(), new CraftItem(((CraftServer) Bukkit.getServer()), (AbstractArrowEntity) (Object) this, item), (AbstractArrow) this.getBukkitEntity());
                Bukkit.getPluginManager().callEvent(event);
                if (event.isCancelled()) {
                    return;
                }
                itemstack = item.getItem();
            }
            boolean flag = this.pickupStatus == AbstractArrowEntity.PickupStatus.ALLOWED || (this.pickupStatus == AbstractArrowEntity.PickupStatus.CREATIVE_ONLY && playerEntity.abilities.isCreativeMode) || (this.getNoClip() && this.func_234616_v_().getUniqueID() == playerEntity.getUniqueID());
            if (this.pickupStatus == AbstractArrowEntity.PickupStatus.ALLOWED && !playerEntity.inventory.addItemStackToInventory(itemstack)) {
                flag = false;
            }
            if (flag) {
                playerEntity.onItemPickup((AbstractArrowEntity) (Object) this, 1);
                this.remove();
            }
        }
    }

    @Inject(method = "setShooter", at = @At("HEAD"))
    private void arclight$setShooter(Entity entityIn, CallbackInfo ci) {
        this.projectileSource = entityIn == null ? null : (ProjectileSource) ((EntityBridge) entityIn).bridge$getBukkitEntity();
    }
}
