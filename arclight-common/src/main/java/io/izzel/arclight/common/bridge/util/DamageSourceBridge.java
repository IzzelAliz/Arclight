package io.izzel.arclight.common.bridge.util;

import net.minecraft.util.DamageSource;

public interface DamageSourceBridge {

    boolean bridge$isSweep();

    DamageSource bridge$sweep();
}
