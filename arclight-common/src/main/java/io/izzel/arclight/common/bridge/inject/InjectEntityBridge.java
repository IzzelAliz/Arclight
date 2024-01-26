package io.izzel.arclight.common.bridge.inject;

import org.bukkit.craftbukkit.v.entity.CraftEntity;

public interface InjectEntityBridge {

    default CraftEntity bridge$getBukkitEntity() {
        throw new IllegalStateException("Not implemented");
    }
}
