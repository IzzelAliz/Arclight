package io.izzel.arclight.common.mixin.core.item;

import io.izzel.arclight.common.bridge.entity.EntityBridge;
import io.izzel.arclight.common.bridge.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.mixin.Eject;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(CrossbowItem.class)
public class CrossbowItemMixin {

    @Inject(method = "fireProjectile", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;damageItem(ILnet/minecraft/entity/LivingEntity;Ljava/util/function/Consumer;)V"))
    private static void arclight$entityShoot(World worldIn, LivingEntity shooter, Hand handIn, ItemStack crossbow, ItemStack projectile, float soundPitch, boolean isCreativeMode, float velocity, float inaccuracy, float projectileAngle, CallbackInfo ci,
                                             boolean flag, ProjectileEntity proj) {
        EntityShootBowEvent event = CraftEventFactory.callEntityShootBowEvent(shooter, crossbow, projectile, proj, shooter.getActiveHand(), soundPitch, true);
        if (event.isCancelled()) {
            event.getProjectile().remove();
            ci.cancel();
        }
        arclight$capturedBoolean = event.getProjectile() == ((EntityBridge) proj).bridge$getBukkitEntity();
    }

    private static transient boolean arclight$capturedBoolean;

    @Eject(method = "fireProjectile", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;addEntity(Lnet/minecraft/entity/Entity;)Z"))
    private static boolean arclight$addEntity(World world, Entity entityIn, CallbackInfo ci, World worldIn, LivingEntity shooter) {
        if (arclight$capturedBoolean) {
            if (!world.addEntity(entityIn)) {
                if (shooter instanceof ServerPlayerEntity) {
                    ((ServerPlayerEntityBridge) shooter).bridge$getBukkitEntity().updateInventory();
                }
                ci.cancel();
            }
        }
        return true;
    }
}
