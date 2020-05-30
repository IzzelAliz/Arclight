package io.izzel.arclight.common.bridge.bukkit;

import org.bukkit.NamespacedKey;

public interface MaterialBridge {

    void bridge$setKey(NamespacedKey namespacedKey);

    void bridge$setInternal(net.minecraft.block.material.Material internal);

    void bridge$setItem();

    void bridge$setBlock();
}
