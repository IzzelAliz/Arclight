package io.izzel.arclight.common.mixin.core.item;

import io.izzel.arclight.common.bridge.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.entity.projectile.TridentEntityBridge;
import io.izzel.arclight.mixin.Eject;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.TridentItem;
import net.minecraft.world.World;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
import org.bukkit.event.player.PlayerRiptideEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(TridentItem.class)
public class TridentItemMixin {

    @Redirect(method = "onPlayerStoppedUsing", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;damageItem(ILnet/minecraft/entity/LivingEntity;Ljava/util/function/Consumer;)V"))
    public void arclight$muteDamage(ItemStack stack, int amount, LivingEntity entityIn, Consumer<LivingEntity> onBroken) {
        int j = EnchantmentHelper.getRiptideModifier(stack);
        if (j != 0) stack.damageItem(amount, entityIn, onBroken);
    }

    @Eject(method = "onPlayerStoppedUsing", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;addEntity(Lnet/minecraft/entity/Entity;)Z"))
    public boolean arclight$addEntity(World world, Entity entityIn, CallbackInfo ci, ItemStack stack, World worldIn, LivingEntity entityLiving, int timeLeft) {
        if (!world.addEntity(entityIn)) {
            if (entityLiving instanceof ServerPlayerEntity) {
                ((ServerPlayerEntityBridge) entityLiving).bridge$getBukkitEntity().updateInventory();
            }
            ci.cancel();
            return false;
        }
        stack.damageItem(1, entityLiving, (entity) ->
            entity.sendBreakAnimation(entityLiving.getActiveHand()));
        ((TridentEntityBridge) entityIn).bridge$setThrownStack(stack.copy());
        return true;
    }

    @Inject(method = "onPlayerStoppedUsing", at = @At(value = "FIELD", ordinal = 1, target = "Lnet/minecraft/entity/player/PlayerEntity;rotationYaw:F"))
    public void arclight$riptide(ItemStack stack, World worldIn, LivingEntity entityLiving, int timeLeft, CallbackInfo ci) {
        PlayerRiptideEvent event = new PlayerRiptideEvent(((ServerPlayerEntityBridge) entityLiving).bridge$getBukkitEntity(), CraftItemStack.asCraftMirror(stack));
        Bukkit.getPluginManager().callEvent(event);
    }
}
