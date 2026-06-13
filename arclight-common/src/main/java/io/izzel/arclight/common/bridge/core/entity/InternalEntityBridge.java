package io.izzel.arclight.common.bridge.core.entity;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import org.bukkit.craftbukkit.entity.CraftEntity;

public interface InternalEntityBridge {

    CraftEntity internal$getBukkitEntity();
}
