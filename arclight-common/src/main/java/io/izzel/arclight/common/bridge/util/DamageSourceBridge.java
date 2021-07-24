package io.izzel.arclight.common.bridge.util;

import net.minecraft.world.damagesource.DamageSource;

public interface DamageSourceBridge {

    boolean bridge$isSweep();

    DamageSource bridge$sweep();
}
