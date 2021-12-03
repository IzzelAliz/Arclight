package io.izzel.arclight.common.mixin.core.world.item;

import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.core.entity.projectile.TridentEntityBridge;
import io.izzel.arclight.common.mod.util.DistValidate;
import io.izzel.arclight.mixin.Eject;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
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

    @Redirect(method = "releaseUsing", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Consumer;)V"))
    public void arclight$muteDamage(ItemStack stack, int amount, LivingEntity entityIn, Consumer<LivingEntity> onBroken) {
        int j = EnchantmentHelper.getRiptide(stack);
        if (j != 0) stack.hurtAndBreak(amount, entityIn, onBroken);
    }

    @Eject(method = "releaseUsing", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    public boolean arclight$addEntity(Level world, Entity entityIn, CallbackInfo ci, ItemStack stack, Level worldIn, LivingEntity entityLiving, int timeLeft) {
        if (!world.addFreshEntity(entityIn)) {
            if (entityLiving instanceof ServerPlayer) {
                ((ServerPlayerEntityBridge) entityLiving).bridge$getBukkitEntity().updateInventory();
            }
            ci.cancel();
            return false;
        }
        stack.hurtAndBreak(1, entityLiving, (entity) ->
            entity.broadcastBreakEvent(entityLiving.getUsedItemHand()));
        ((TridentEntityBridge) entityIn).bridge$setThrownStack(stack.copy());
        return true;
    }

    @Inject(method = "releaseUsing", at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/world/entity/player/Player;getYRot()F"))
    public void arclight$riptide(ItemStack stack, Level worldIn, LivingEntity entityLiving, int timeLeft, CallbackInfo ci) {
        if (!DistValidate.isValid(worldIn)) return;
        PlayerRiptideEvent event = new PlayerRiptideEvent(((ServerPlayerEntityBridge) entityLiving).bridge$getBukkitEntity(), CraftItemStack.asCraftMirror(stack));
        Bukkit.getPluginManager().callEvent(event);
    }
}
