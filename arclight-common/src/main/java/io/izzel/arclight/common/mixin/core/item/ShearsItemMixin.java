package io.izzel.arclight.common.mixin.core.item;

import io.izzel.arclight.common.bridge.entity.EntityBridge;
import io.izzel.arclight.common.bridge.entity.player.ServerPlayerEntityBridge;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShearsItem;
import net.minecraft.util.Hand;
import org.bukkit.Bukkit;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShearsItem.class)
public class ShearsItemMixin {

    @Inject(method = "itemInteractionForEntity", cancellable = true, at = @At(value = "INVOKE", remap = false, target = "Lnet/minecraftforge/common/IShearable;isShearable(Lnet/minecraft/item/ItemStack;Lnet/minecraft/world/IWorldReader;Lnet/minecraft/util/math/BlockPos;)Z"))
    private void arclight$onShear(ItemStack stack, PlayerEntity playerIn, LivingEntity entity, Hand hand, CallbackInfoReturnable<Boolean> cir) {
        if (playerIn instanceof ServerPlayerEntityBridge) {
            PlayerShearEntityEvent event = new PlayerShearEntityEvent(((ServerPlayerEntityBridge) playerIn).bridge$getBukkitEntity(), ((EntityBridge) entity).bridge$getBukkitEntity());
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                cir.setReturnValue(false);
            }
        }
    }
}
