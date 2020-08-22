package io.izzel.arclight.common.mixin.core.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.FlintAndSteelItem;
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

@Mixin(FlintAndSteelItem.class)
public class FlintAndSteelItemMixin {

    @Inject(method = "onItemUse", cancellable = true, at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/world/World;playSound(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/SoundEvent;Lnet/minecraft/util/SoundCategory;FF)V"))
    public void arclight$blockIgnite(ItemUseContext context, CallbackInfoReturnable<ActionResultType> cir) {
        PlayerEntity playerentity = context.getPlayer();
        World world = context.getWorld();
        BlockPos blockpos = context.getPos();
        BlockPos blockpos1 = blockpos.offset(context.getFace());
        if (CraftEventFactory.callBlockIgniteEvent(world, blockpos1, BlockIgniteEvent.IgniteCause.FLINT_AND_STEEL, playerentity).isCancelled()) {
            context.getItem().damageItem(1, playerentity, (entity) -> {
                entity.sendBreakAnimation(context.getHand());
            });
            cir.setReturnValue(ActionResultType.PASS);
        }
    }
}
