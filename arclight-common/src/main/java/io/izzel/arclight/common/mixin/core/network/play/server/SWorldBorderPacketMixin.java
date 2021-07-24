package io.izzel.arclight.common.mixin.core.network.play.server;

import io.izzel.arclight.common.bridge.world.border.WorldBorderBridge;
import net.minecraft.network.protocol.game.ClientboundSetBorderPacket;
import net.minecraft.world.level.border.WorldBorder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientboundSetBorderPacket.class)
public class SWorldBorderPacketMixin {

    // @formatter:off
    @Shadow private double newCenterX;
    @Shadow private double newCenterZ;
    // @formatter:on

    @Inject(method = "<init>(Lnet/minecraft/world/level/border/WorldBorder;Lnet/minecraft/network/protocol/game/ClientboundSetBorderPacket$Type;)V", at = @At("RETURN"))
    private void arclight$nether(WorldBorder border, ClientboundSetBorderPacket.Type actionIn, CallbackInfo ci) {
        this.newCenterX = border.getCenterX() * (((WorldBorderBridge) border).bridge$getWorld().dimensionType().coordinateScale());
        this.newCenterZ = border.getCenterZ() * (((WorldBorderBridge) border).bridge$getWorld().dimensionType().coordinateScale());
    }
}
