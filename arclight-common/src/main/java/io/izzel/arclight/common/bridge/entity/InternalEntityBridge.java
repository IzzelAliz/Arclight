package io.izzel.arclight.common.bridge.entity;

import net.minecraft.core.BlockPos;
import org.bukkit.craftbukkit.v.entity.CraftEntity;

public interface InternalEntityBridge {

    CraftEntity internal$getBukkitEntity();

    BlockPos internal$capturedPos();
}
