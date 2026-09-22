package io.izzel.arclight.common.mixin.core.network;

import io.izzel.arclight.common.bridge.core.server.network.ServerGamePacketListenerImplBridge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.phys.Vec3;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(value = ServerGamePacketListenerImpl.class, priority = 1500)
public abstract class ServerGamePacketListenerImplMixin_LowPriority implements ServerGamePacketListenerImplBridge {

    @Shadow private ServerPlayer player;
    @Shadow private Vec3 awaitingPositionFromClient;

    @Inject(method = "teleport(Lnet/minecraft/world/entity/PositionMoveRotation;Ljava/util/Set;)V", at = @At(value = "FIELD", shift = At.Shift.AFTER, opcode = Opcodes.PUTFIELD, target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;awaitingPositionFromClient:Lnet/minecraft/world/phys/Vec3;"))
    private void arclight$storeLastPosition(PositionMoveRotation positionMoveRotation, Set<Relative> set, CallbackInfo ci) {
        arclight$platform$setLastPosX(this.awaitingPositionFromClient.x);
        arclight$platform$setLastPosY(this.awaitingPositionFromClient.y);
        arclight$platform$setLastPosZ(this.awaitingPositionFromClient.z);
        arclight$platform$setLastYaw(this.player.getYRot());
        arclight$platform$setLastPitch(this.player.getXRot());
    }
}
