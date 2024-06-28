package io.izzel.arclight.common.mixin.core.server.level;

import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ServerPlayer.RespawnPosAngle.class)
public class ServerPlayer_RespawnPosAngleMixin implements ServerPlayerEntityBridge.RespawnPosAngleBridge {

    @Unique private boolean isBedSpawn;
    @Unique private boolean isAnchorSpawn;

    @Override
    public boolean bridge$isBedSpawn() {
        return isBedSpawn;
    }

    @Override
    public boolean bridge$isAnchorSpawn() {
        return isAnchorSpawn;
    }

    @Override
    public void bridge$setBedSpawn(boolean b) {
        isBedSpawn = b;
    }

    @Override
    public void bridge$setAnchorSpawn(boolean b) {
        isAnchorSpawn = b;
    }
}
