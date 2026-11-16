package io.izzel.arclight.common.bridge.core.world.item;

import net.minecraft.resources.Identifier;

public interface ItemCooldownsBridge {
    int bridge$getRemainingCooldown(Identifier cooldownGroup);
}
