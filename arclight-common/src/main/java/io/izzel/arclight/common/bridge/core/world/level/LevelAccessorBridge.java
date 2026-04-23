package io.izzel.arclight.common.bridge.core.world.level;

import io.izzel.arclight.common.mod.util.DistValidate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;

public interface LevelAccessorBridge {
    static LevelAccessorBridge from(LevelAccessor world) {
        final var result = (LevelAccessorBridge) world;
        return result.arclight$isActual() ? result : null;
    }

    default boolean arclight$isActual() {
        return DistValidate.isValid((LevelAccessor) this);
    }

    default ServerLevel bridge$getMinecraftWorld() {
        throw new UnsupportedOperationException(String.format("No server level found for %s.\n This is likely because the specified world is not a ServerLevelAccessor and thus it shouldn't be a logic world.\n Otherwise it is a bug of Arclight.", getClass().getName()));
    }
}
