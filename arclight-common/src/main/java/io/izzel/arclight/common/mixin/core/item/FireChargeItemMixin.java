package io.izzel.arclight.common.mixin.core.item;

import net.minecraft.item.FireChargeItem;
import net.minecraft.item.ItemUseContext;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.block.BlockIgniteEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(FireChargeItem.class)
public class FireChargeItemMixin {

    @Inject(method = "onItemUse", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/item/FireChargeItem;playUseSound(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)V"))
    public void arclight$blockIgnite(ItemUseContext context, CallbackInfoReturnable<ActionResultType> cir, World world, BlockPos blockPos) {
        if (CraftEventFactory.callBlockIgniteEvent(world, blockPos, BlockIgniteEvent.IgniteCause.FIREBALL, context.getPlayer()).isCancelled()) {
            if (!context.getPlayer().abilities.isCreativeMode) {
                context.getItem().shrink(1);
            }
            cir.setReturnValue(ActionResultType.PASS);
        }
    }
}
