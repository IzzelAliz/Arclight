package io.izzel.arclight.common.mixin.core.network;

import io.izzel.arclight.common.bridge.core.network.ConnectionSpawnLocationEventBridge;
import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Connection.class)
public abstract class ConnectionSpawnLocationEventMixin implements ConnectionSpawnLocationEventBridge {

    @Unique private boolean arclight$legacySpawnLocationHandled;

    @Override
    public boolean bridge$isLegacySpawnLocationHandled() {
        return this.arclight$legacySpawnLocationHandled;
    }

    @Override
    public void bridge$setLegacySpawnLocationHandled(boolean handled) {
        this.arclight$legacySpawnLocationHandled = handled;
    }
}
