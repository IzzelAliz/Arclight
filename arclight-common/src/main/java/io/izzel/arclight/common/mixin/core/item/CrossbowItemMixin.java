package io.izzel.arclight.common.mixin.core.item;

import io.izzel.arclight.common.bridge.entity.EntityBridge;
import io.izzel.arclight.common.bridge.entity.player.ServerPlayerEntityBridge;
import net.minecraft.entity.Entity;
import net.minecraft.entity.IProjectile;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(CrossbowItem.class)
public class CrossbowItemMixin {

    @Inject(method = "fireProjectile", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;damageItem(ILnet/minecraft/entity/LivingEntity;Ljava/util/function/Consumer;)V"))
    private static void arclight$entityShoot(World worldIn, LivingEntity shooter, Hand handIn, ItemStack crossbow, ItemStack projectile, float soundPitch, boolean isCreativeMode, float velocity, float inaccuracy, float projectileAngle, CallbackInfo ci,
                                             boolean flag, IProjectile proj) {
        EntityShootBowEvent event = CraftEventFactory.callEntityShootBowEvent(shooter, crossbow, (Entity) proj, soundPitch);
        if (event.isCancelled()) {
            event.getProjectile().remove();
            ci.cancel();
        }
        arclight$capturedBoolean = event.getProjectile() == ((EntityBridge) proj).bridge$getBukkitEntity();
    }

    private static transient boolean arclight$capturedBoolean;

    @Redirect(method = "fireProjectile", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;addEntity(Lnet/minecraft/entity/Entity;)Z"))
    private static boolean arclight$addEntity(World world, Entity entityIn, World worldIn, LivingEntity shooter) {
        if (arclight$capturedBoolean) {
            if (!world.addEntity(entityIn)) {
                if (shooter instanceof ServerPlayerEntity) {
                    ((ServerPlayerEntityBridge) shooter).bridge$getBukkitEntity().updateInventory();
                }
                arclight$capturedBoolean = true;
            } else {
                arclight$capturedBoolean = false;
            }
        }
        return true;
    }

    @Inject(method = "fireProjectile", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;playSound(Lnet/minecraft/entity/player/PlayerEntity;DDDLnet/minecraft/util/SoundEvent;Lnet/minecraft/util/SoundCategory;FF)V"))
    private static void arclight$returnIfFail(World worldIn, LivingEntity shooter, Hand handIn, ItemStack crossbow, ItemStack projectile, float soundPitch, boolean isCreativeMode, float velocity, float inaccuracy, float projectileAngle, CallbackInfo ci) {
        if (arclight$capturedBoolean) {
            ci.cancel();
        }
        arclight$capturedBoolean = false;
    }
}
