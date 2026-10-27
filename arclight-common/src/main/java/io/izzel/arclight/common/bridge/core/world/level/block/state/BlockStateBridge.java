package io.izzel.arclight.common.bridge.core.world.level.block.state;

import org.bukkit.Material;
import org.bukkit.craftbukkit.block.data.CraftBlockData;

public interface BlockStateBridge {

    default CraftBlockData asBlockData() {
        throw new UnsupportedOperationException("Implemented by BlockStateMixin");
    }

    default Material getBukkitMaterial() {
        throw new UnsupportedOperationException("Implemented by BlockStateMixin");
    }
}
