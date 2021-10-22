package io.izzel.arclight.common.mixin.core.world.item;

import io.izzel.arclight.common.mod.util.DistValidate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.block.BlockIgniteEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FlintAndSteelItem.class)
public class FlintAndSteelItemMixin {

    @Inject(method = "useOn", cancellable = true, at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/world/level/Level;playSound(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/BlockPos;Lnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V"))
    public void arclight$blockIgnite(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (!DistValidate.isValid(context)) return;
        Player playerentity = context.getPlayer();
        Level world = context.getLevel();
        BlockPos blockpos = context.getClickedPos();
        BlockPos blockpos1 = blockpos.relative(context.getClickedFace());
        if (CraftEventFactory.callBlockIgniteEvent(world, blockpos1, BlockIgniteEvent.IgniteCause.FLINT_AND_STEEL, playerentity).isCancelled()) {
            context.getItemInHand().hurtAndBreak(1, playerentity, (entity) -> {
                entity.broadcastBreakEvent(context.getHand());
            });
            cir.setReturnValue(InteractionResult.PASS);
        }
    }
}
