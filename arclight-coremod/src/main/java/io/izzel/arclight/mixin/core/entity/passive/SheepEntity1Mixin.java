package io.izzel.arclight.mixin.core.entity.passive;

import io.izzel.arclight.bridge.inventory.container.ContainerBridge;
import org.bukkit.inventory.InventoryView;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(targets = "net/minecraft/entity/passive/SheepEntity$1")
public abstract class SheepEntity1Mixin implements ContainerBridge {

    @Override
    public InventoryView bridge$getBukkitView() {
        return null;
    }
}
