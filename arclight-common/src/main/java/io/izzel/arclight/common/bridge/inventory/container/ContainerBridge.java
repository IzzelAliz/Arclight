package io.izzel.arclight.common.bridge.inventory.container;

import net.minecraft.inventory.container.Container;
import net.minecraft.util.text.ITextComponent;
import org.bukkit.craftbukkit.v.entity.CraftHumanEntity;
import org.bukkit.inventory.InventoryView;

public interface ContainerBridge {

    InventoryView bridge$getBukkitView();

    void bridge$transferTo(Container other, CraftHumanEntity player);

    ITextComponent bridge$getTitle();

    void bridge$setTitle(ITextComponent title);

    boolean bridge$isCheckReachable();
}
