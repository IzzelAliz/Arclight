package io.izzel.arclight.common.mixin.forge;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemUseContext;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import io.izzel.arclight.common.mod.util.ArclightCaptures;

@Mixin(ForgeHooks.class)
public class ForgeHooksMixin {

    @Inject(method = "onPlaceItemIntoWorld", remap = false, at = @At("HEAD"))
    private static void arclight$captureHand(ItemUseContext context, CallbackInfoReturnable<ActionResultType> cir) {
        ArclightCaptures.capturePlaceEventHand(context.getHand());
    }

    @Inject(method = "onPlaceItemIntoWorld", remap = false, at = @At("RETURN"))
    private static void arclight$removeHand(ItemUseContext context, CallbackInfoReturnable<ActionResultType> cir) {
        ArclightCaptures.getPlaceEventHand(Hand.MAIN_HAND);
    }

    @Inject(method = "canEntityDestroy", cancellable = true, remap = false, at = @At("HEAD"))
    private static void arclight$returnIfNotLoaded(World world, BlockPos pos, LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!world.getChunkProvider().isChunkLoaded(new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4))) {
            cir.setReturnValue(false);
        }
    }
}
