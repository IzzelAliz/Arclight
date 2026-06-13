package io.izzel.arclight.common.bridge.core.world.level;

import net.minecraft.world.level.gamerules.GameRule;

import java.util.Set;

public interface GameRulesBridge {
    Set<GameRule<?>> arclight$getAllRules();
}
