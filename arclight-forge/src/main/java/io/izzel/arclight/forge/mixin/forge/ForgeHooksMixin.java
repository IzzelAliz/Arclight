package io.izzel.arclight.forge.mixin.forge;

import io.izzel.arclight.common.bridge.bukkit.MessengerBridge;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.network.CustomPayloadEvent;
import org.bukkit.Bukkit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ForgeHooks.class)
public class ForgeHooksMixin {

    @Inject(method = "onPlaceItemIntoWorld", remap = false, at = @At("HEAD"))
    private static void arclight$captureHand(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        ArclightCaptures.capturePlaceEventHand(context.getHand());
    }

    @Inject(method = "onPlaceItemIntoWorld", remap = false, at = @At("RETURN"))
    private static void arclight$removeHand(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        ArclightCaptures.getPlaceEventHand(InteractionHand.MAIN_HAND);
    }

    @Inject(method = "onCustomPayload(Lnet/minecraftforge/event/network/CustomPayloadEvent;)Z", at = @At("RETURN"))
    private static void arclight$recordUnknown(CustomPayloadEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) {
            var recorder = ((MessengerBridge) Bukkit.getMessenger()).arclight$getPacketRecorder();
            recorder.recordUnknown(event.getChannel().toString());
            recorder.update();
        }
    }
}
