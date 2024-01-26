package io.izzel.arclight.common.bridge.inject;

import org.bukkit.craftbukkit.v.CraftWorld;

public interface InjectLevelBridge {

    default CraftWorld bridge$getWorld() {
        throw new IllegalStateException("Not implemented");
    }
}
