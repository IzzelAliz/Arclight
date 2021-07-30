package io.izzel.arclight.common.mixin.core.world.entity.projectile;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.entity.player.PlayerInventoryBridge;
import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.entity.CraftItem;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.player.PlayerPickupArrowEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.world.entity.projectile.AbstractArrow.class)
public abstract class AbstractArrowMixin extends ProjectileMixin {

    // @formatter:off
    @Shadow public boolean inGround;
    @Shadow public abstract boolean isNoPhysics();
    @Shadow public int shakeTime;
    @Shadow public net.minecraft.world.entity.projectile.AbstractArrow.Pickup pickup;
    @Shadow protected abstract ItemStack getPickupItem();
    // @formatter:on

    @Redirect(method = "tick", at = @At(value = "INVOKE", opcode = Opcodes.PUTFIELD, target = "Lnet/minecraft/world/entity/projectile/AbstractArrow;onHit(Lnet/minecraft/world/phys/HitResult;)V"))
    private void arclight$hitEvent(net.minecraft.world.entity.projectile.AbstractArrow abstractArrow, HitResult hitResult) {
        this.preOnHit(hitResult);
    }

    @Redirect(method = "onHitEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;setSecondsOnFire(I)V"))
    private void arclight$fireShot(Entity entity, int seconds, EntityHitResult result) {
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
    public void playerTouch(Player playerEntity) {
        if (!this.level.isClientSide && (this.inGround || this.isNoPhysics()) && this.shakeTime <= 0) {
            ItemStack itemstack = this.getPickupItem();
            if (this.pickup == net.minecraft.world.entity.projectile.AbstractArrow.Pickup.ALLOWED && !itemstack.isEmpty() && ((PlayerInventoryBridge) playerEntity.getInventory()).bridge$canHold(itemstack) > 0) {
                ItemEntity item = new ItemEntity(this.level, this.getX(), this.getY(), this.getZ(), itemstack);
                PlayerPickupArrowEvent event = new PlayerPickupArrowEvent(((ServerPlayerEntityBridge) playerEntity).bridge$getBukkitEntity(), new CraftItem(((CraftServer) Bukkit.getServer()), (net.minecraft.world.entity.projectile.AbstractArrow) (Object) this, item), (AbstractArrow) this.getBukkitEntity());
                Bukkit.getPluginManager().callEvent(event);
                if (event.isCancelled()) {
                    return;
                }
                itemstack = item.getItem();
            }
            if ((this.pickup == net.minecraft.world.entity.projectile.AbstractArrow.Pickup.ALLOWED && playerEntity.getInventory().add(itemstack)) || (this.pickup == net.minecraft.world.entity.projectile.AbstractArrow.Pickup.CREATIVE_ONLY && playerEntity.getAbilities().instabuild)) {
                playerEntity.take((net.minecraft.world.entity.projectile.AbstractArrow) (Object) this, 1);
                this.discard();
            }
        }
    }

    @Inject(method = "setOwner", at = @At("HEAD"))
    private void arclight$setShooter(Entity entityIn, CallbackInfo ci) {
        this.projectileSource = entityIn == null ? null : (ProjectileSource) ((EntityBridge) entityIn).bridge$getBukkitEntity();
    }
}
