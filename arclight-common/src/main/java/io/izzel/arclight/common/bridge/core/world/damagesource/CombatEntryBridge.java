package io.izzel.arclight.common.bridge.core.world.damagesource;

import net.minecraft.network.chat.Component;

public interface CombatEntryBridge {

    void bridge$setDeathMessage(Component component);

    Component bridge$getDeathMessage();
}
