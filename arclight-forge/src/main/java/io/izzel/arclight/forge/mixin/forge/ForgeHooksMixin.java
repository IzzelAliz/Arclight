package io.izzel.arclight.forge.mixin.forge;

import io.izzel.arclight.common.mod.plugin.messaging.PacketRecorder;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.network.CustomPayloadEvent;
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
            ((io.izzel.arclight.common.bridge.bukkit.MessengerBridge) org.bukkit.Bukkit.getMessenger()).arclight$getPacketRecorder().recordUnknown(event.getChannel());
        }
    }

    @io.izzel.arclight.mixin.Decorate(method = "onBlockBreakEvent", remap = false,
        at = @At(value = "INVOKE", target = "Lnet/minecraftforge/eventbus/api/bus/CancellableEventBus;fire(Lnet/minecraftforge/eventbus/internal/Event;)Lnet/minecraftforge/eventbus/internal/Event;"))
    private static net.minecraftforge.eventbus.internal.Event arclight$blockBreak(
            net.minecraftforge.eventbus.api.bus.CancellableEventBus<?> bus,
            net.minecraftforge.eventbus.internal.Event event) throws Throwable {
        net.minecraftforge.eventbus.internal.Event fired = (net.minecraftforge.eventbus.internal.Event)
            io.izzel.arclight.mixin.DecorationOps.callsite().invoke(bus, event);
        var result = (net.minecraftforge.event.level.BlockEvent.BreakEvent) fired;
        boolean cancelled = io.izzel.arclight.forge.mod.event.BlockBreakEventDispatcher.onBreakBlock(result, result.getResult().isDenied());
        result.setResult(cancelled ? net.minecraftforge.common.util.Result.DENY : net.minecraftforge.common.util.Result.ALLOW);
        return result;
    }

    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(
        method = "onPlaceItemIntoWorld(Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;",
        remap = false,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;awardStat(Lnet/minecraft/stats/Stat;)V"),
        require = 1, allow = 1)
    private static void arclight$deferBonemealStat(net.minecraft.world.entity.player.Player player,
            net.minecraft.stats.Stat<?> stat, com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original) {
        io.izzel.arclight.common.mod.util.TreeCapture.award(player, () -> original.call(player, stat));
    }
}
