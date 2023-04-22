package io.izzel.arclight.common.mixin.core.world.inventory;

import io.izzel.arclight.common.bridge.core.inventory.container.LecternContainerBridge;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.LecternMenu;
import net.minecraft.world.inventory.MenuType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MenuType.class)
public class ContainerTypeMixin {

    @Inject(method = "register(Ljava/lang/String;Lnet/minecraft/world/inventory/MenuType$MenuSupplier;)Lnet/minecraft/world/inventory/MenuType;", cancellable = true, at = @At("HEAD"))
    private static <T extends AbstractContainerMenu> void arclight$replaceLectern(String key, MenuType.MenuSupplier<T> factory, CallbackInfoReturnable<MenuType<T>> cir) {
        if (key.equals("lectern")) {
            cir.setReturnValue(Registry.register(BuiltInRegistries.MENU, key, new MenuType<>((i, inv) -> {
                LecternMenu container = new LecternMenu(i);
                ((LecternContainerBridge) container).bridge$setPlayerInventory(inv);
                return (T) container;
            }, FeatureFlags.VANILLA_SET)));
        }
    }
}
