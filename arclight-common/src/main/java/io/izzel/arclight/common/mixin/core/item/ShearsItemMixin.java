package io.izzel.arclight.common.mixin.core.item;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShearsItem;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShearsItem.class)
public class ShearsItemMixin {

    @Inject(method = "itemInteractionForEntity", cancellable = true, at = @At(value = "INVOKE", remap = false, target = "Lnet/minecraftforge/common/IForgeShearable;isShearable(Lnet/minecraft/item/ItemStack;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)Z"))
    private void arclight$onShear(ItemStack stack, PlayerEntity playerIn, LivingEntity entity, Hand hand, CallbackInfoReturnable<ActionResultType> cir) {
        if (!CraftEventFactory.handlePlayerShearEntityEvent(playerIn, entity, stack, hand)) {
            cir.setReturnValue(ActionResultType.PASS);
        }
    }
}
