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
import net.minecraftforge.eventbus.api.Event;
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

    @Decorate(method = "onLivingDrops", remap = false, at = @At(value = "INVOKE", target = "Lnet/minecraftforge/event/ForgeEventFactory;post(Lnet/minecraftforge/eventbus/api/Event;)Z"))
    private static boolean arclight$monitorLivingDrops(Event e) throws Throwable {
        LivingDropsEvent event = (LivingDropsEvent) e;
        boolean result = (boolean) DecorationOps.callsite().invoke(e);
        return EntityEventHandler.monitorLivingDrops(event.getEntity(), event.getSource(), (List<ItemEntity>) event.getDrops(), result);
    }
}
