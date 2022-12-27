package io.izzel.arclight.common.mixin.core.world.item;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.mod.util.DistValidate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(CrossbowItem.class)
public class CrossbowItemMixin {

    @Inject(method = "shootProjectile", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Consumer;)V"))
    private static void arclight$entityShoot(Level worldIn, LivingEntity shooter, InteractionHand handIn, ItemStack crossbow, ItemStack projectile, float soundPitch, boolean isCreativeMode, float velocity, float inaccuracy, float projectileAngle, CallbackInfo ci,
                                             boolean flag, Projectile proj) {
        if (!DistValidate.isValid(worldIn)) {
            return;
        }
        EntityShootBowEvent event = CraftEventFactory.callEntityShootBowEvent(shooter, crossbow, projectile, proj, shooter.getUsedItemHand(), soundPitch, true);
        if (event.isCancelled()) {
            event.getProjectile().remove();
            ci.cancel();
            return;
        }
        if (event.getProjectile() != ((EntityBridge) proj).bridge$getBukkitEntity()) {
            worldIn.playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(), SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1.0F, soundPitch);
            ci.cancel();
        }
    }

    @Inject(method = "shootProjectile", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private static void arclight$addEntity(Level p_40895_, LivingEntity shooter, InteractionHand p_40897_, ItemStack p_40898_, ItemStack p_40899_, float p_40900_, boolean p_40901_, float p_40902_, float p_40903_, float p_40904_, CallbackInfo ci,
                                           boolean b, Projectile projectile) {
        if (!projectile.isAddedToWorld()) {
            if (shooter instanceof ServerPlayer) {
                ((ServerPlayerEntityBridge) shooter).bridge$getBukkitEntity().updateInventory();
            }
            ci.cancel();
        }
    }
}
