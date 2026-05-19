package io.izzel.arclight.common.bridge.core.world.level;

import net.minecraft.server.level.ServerLevel;

import javax.annotation.Nullable;

public interface GameRules_ValueBridge<T> {
    void arclight$setFrom(T t, @Nullable ServerLevel level);
    void arclight$set(Object value, @Nullable ServerLevel level);
}
