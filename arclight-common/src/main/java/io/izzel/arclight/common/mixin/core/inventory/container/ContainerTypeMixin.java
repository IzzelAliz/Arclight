package io.izzel.arclight.common.mixin.core.inventory.container;

import io.izzel.arclight.common.bridge.inventory.container.LecternContainerBridge;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.inventory.container.LecternContainer;
import net.minecraft.util.registry.Registry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ContainerType.class)
public class ContainerTypeMixin<T extends Container> {

    @Inject(method = "register", cancellable = true, at = @At("HEAD"))
    private static <T extends Container> void arclight$replaceLectern(String key, ContainerType.IFactory<T> factory, CallbackInfoReturnable<ContainerType<T>> cir) {
        if (key.equals("lectern")) {
            cir.setReturnValue(Registry.register(Registry.MENU, key, new ContainerType<>((i, inv) -> {
                LecternContainer container = new LecternContainer(i);
                ((LecternContainerBridge) container).bridge$setPlayerInventory(inv);
                return (T) container;
            })));
        }
    }
}
