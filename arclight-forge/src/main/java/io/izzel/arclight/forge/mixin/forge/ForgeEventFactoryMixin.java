package io.izzel.arclight.forge.mixin.forge;

import io.izzel.arclight.common.mod.server.event.EntityEventHandler;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.internal.Event;
import net.minecraftforge.eventbus.api.bus.CancellableEventBus;
import io.izzel.arclight.forge.mod.event.BlockPlaceEventDispatcher;
import io.izzel.arclight.forge.mod.event.EntityTeleportEventDispatcher;
import io.izzel.arclight.forge.mod.event.ItemEntityEventDispatcher;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.item.ItemExpireEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import io.izzel.arclight.common.mod.util.ArclightCaptures;

import java.util.List;

@Mixin(ForgeEventFactory.class)
public class ForgeEventFactoryMixin {

    @Inject(method = "onBlockPlace", remap = false, at = @At("HEAD"))
    private static void arclight$captureDirection(Entity entity, BlockSnapshot blockSnapshot, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        ArclightCaptures.capturePlaceEventDirection(direction);
    }

    @Inject(method = "onMultiBlockPlace", remap = false, at = @At("HEAD"))
    private static void arclight$captureDirection(Entity entity, List<BlockSnapshot> blockSnapshots, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        ArclightCaptures.capturePlaceEventDirection(direction);
    }

    @Decorate(method = "onLivingDrops", remap = false, at = @At(value = "INVOKE", target = "Lnet/minecraftforge/eventbus/api/bus/CancellableEventBus;post(Lnet/minecraftforge/eventbus/internal/Event;)Z"))
    private static boolean arclight$monitorLivingDrops(CancellableEventBus<?> bus, Event e) throws Throwable {
        LivingDropsEvent event = (LivingDropsEvent) e;
        boolean result = (boolean) DecorationOps.callsite().invoke(bus, e);
        return EntityEventHandler.monitorLivingDrops(event.getEntity(), event.getSource(), (List<ItemEntity>) event.getDrops(), result);
    }

    @Decorate(method = {"onBlockPlace", "onMultiBlockPlace", "onItemExpire", "onEnderManTeleport"}, remap = false,
        at = @At(value = "INVOKE", target = "Lnet/minecraftforge/eventbus/api/bus/CancellableEventBus;post(Lnet/minecraftforge/eventbus/internal/Event;)Z"))
    private static boolean arclight$dispatchAfterForge(CancellableEventBus<?> bus, Event event) throws Throwable {
        boolean cancelled = (boolean) DecorationOps.callsite().invoke(bus, event);
        if (event instanceof BlockEvent.EntityMultiPlaceEvent place) {
            return BlockPlaceEventDispatcher.onMultiPlace(place, cancelled);
        }
        if (event instanceof BlockEvent.EntityPlaceEvent place) {
            return BlockPlaceEventDispatcher.onBlockPlace(place, cancelled);
        }
        if (event instanceof ItemExpireEvent expire) {
            return ItemEntityEventDispatcher.onExpire(expire, cancelled);
        }
        if (event instanceof EntityTeleportEvent.EnderEntity teleport) {
            return EntityTeleportEventDispatcher.onTeleport(teleport, cancelled);
        }
        return cancelled;
    }
}
