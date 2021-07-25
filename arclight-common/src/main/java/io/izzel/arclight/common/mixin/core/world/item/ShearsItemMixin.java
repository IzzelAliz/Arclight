package io.izzel.arclight.common.mixin.core.world.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShearsItem.class)
public class ShearsItemMixin {

    @Inject(method = "interactLivingEntity", cancellable = true, at = @At(value = "INVOKE", remap = false, target = "Lnet/minecraftforge/common/IForgeShearable;isShearable(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Z"))
    private void arclight$onShear(ItemStack stack, Player playerIn, LivingEntity entity, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (!CraftEventFactory.handlePlayerShearEntityEvent(playerIn, entity, stack, hand)) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }
}
