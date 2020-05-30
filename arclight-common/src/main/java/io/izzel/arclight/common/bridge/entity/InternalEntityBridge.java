package io.izzel.arclight.common.bridge.entity;

import org.bukkit.craftbukkit.v.entity.CraftEntity;

public interface InternalEntityBridge {

    CraftEntity internal$getBukkitEntity();
}
