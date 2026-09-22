package io.izzel.arclight.common.mixin.core.world.item;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Cancellable;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import io.izzel.arclight.common.bridge.core.world.entity.projectile.ThrownTridentBridge;
import io.izzel.arclight.common.bridge.core.server.level.ServerPlayerBridge;
import io.izzel.arclight.common.mod.util.DistValidate;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.level.Level;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TridentItem.class)
public class TridentItemMixin {

    @Decorate(method = "releaseUsing", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;hurtWithoutBreaking(ILnet/minecraft/world/entity/player/Player;)V"))
    private void arclight$muteDamage(ItemStack instance, int i, Player player, @Local(ordinal = -1) float f) throws Throwable {
        if (f != 0) {
            DecorationOps.callsite().invoke(instance, i, player);
        }
    }

    @WrapOperation(method = "releaseUsing", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;consumeAndReturn(ILnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack arclight$deferConsumption(ItemStack stack, int amount, LivingEntity entity, Operation<ItemStack> original,
            @Share("arclight$consume") LocalRef<Operation<ItemStack>> consume) {
        consume.set(original);
        return stack;
    }

    @WrapOperation(method = "releaseUsing", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/Projectile;spawnProjectileFromRotation(Lnet/minecraft/world/entity/projectile/Projectile$ProjectileFactory;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;FFF)Lnet/minecraft/world/entity/projectile/Projectile;"))
    private Projectile arclight$addEntity(Projectile.ProjectileFactory<? extends Projectile> factory, ServerLevel serverLevel,
            ItemStack itemStack, LivingEntity livingEntity, float xRot, float velocity, float inaccuracy, Operation<Projectile> original,
            ItemStack stack, Level worldIn, LivingEntity entityLiving, int timeLeft,
            @Share("arclight$consume") LocalRef<Operation<ItemStack>> consume,
            @Cancellable CallbackInfoReturnable<Boolean> cir) {
        Projectile entityIn = original.call(factory, serverLevel, itemStack, livingEntity, xRot, velocity, inaccuracy);
        if (entityIn.isRemoved()) {
            if (entityLiving instanceof ServerPlayerBridge player) {
                player.bridge$getBukkitEntity().updateInventory();
            }
            cir.setReturnValue(false);
            return entityIn;
        }
        stack.hurtWithoutBreaking(1, (Player) entityLiving);
        ItemStack consumed = consume.get().call(stack, 1, entityLiving);
        ((ThrownTridentBridge) entityIn).bridge$setThrownStack(consumed.copy());
        return entityIn;
    }

    @Redirect(method = "releaseUsing", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;push(DDD)V"))
    private void arclight$riptide(Player instance, double x, double y, double z, ItemStack stack, Level worldIn, LivingEntity entityLiving, int timeLeft) {
        if (!DistValidate.isValid(worldIn)) return;
        CraftEventFactory.callPlayerRiptideEvent(instance, stack, (float) x, (float) y, (float) z);
        instance.push(x, y, z);
    }
}
