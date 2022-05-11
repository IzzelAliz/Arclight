package io.izzel.arclight.common.mixin.optimization.general.network;

import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin_Optimize implements ServerPlayerEntityBridge {

    @Unique private boolean trackerDirty;

    @Override
    public boolean bridge$isTrackerDirty() {
        return this.trackerDirty;
    }

    @Override
    public void bridge$setTrackerDirty(boolean flag) {
        this.trackerDirty = flag;
    }
}
