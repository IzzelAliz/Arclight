package io.izzel.arclight.bridge.entity;

import org.bukkit.craftbukkit.v1_14_R1.entity.CraftEntity;

public interface InternalEntityBridge {

    CraftEntity internal$getBukkitEntity();
}
