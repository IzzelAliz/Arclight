package io.izzel.arclight.common.mixin.core.world.entity.animal;

import io.izzel.arclight.common.bridge.core.inventory.container.ContainerBridge;
import org.bukkit.inventory.InventoryView;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(targets = "net/minecraft/world/entity/animal/Sheep$1")
public abstract class Sheep1Mixin implements ContainerBridge {

    @Override
    public InventoryView bridge$getBukkitView() {
        return null;
    }
}
