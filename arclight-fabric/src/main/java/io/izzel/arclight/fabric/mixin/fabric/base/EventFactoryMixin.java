package io.izzel.arclight.fabric.mixin.fabric.base;

import io.izzel.arclight.fabric.mod.event.FabricEventAdaptor;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.fabricmc.fabric.api.event.EventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Function;

@Mixin(EventFactory.class)
public class EventFactoryMixin {

    @Decorate(method = "createArrayBacked(Ljava/lang/Class;Ljava/util/function/Function;)Lnet/fabricmc/fabric/api/event/Event;", inject = true, at = @At("HEAD"), remap = false)
    private static<T> void arclight$fabric$injectMonitor(Class<? super T> type, Function<T[], T> invokerFactory) throws Throwable {
        invokerFactory = FabricEventAdaptor.monitored(type, invokerFactory);
        DecorationOps.blackhole().invoke(invokerFactory);
    }
}
