package io.izzel.arclight.common.mixin.core.network.play.server;

import io.izzel.arclight.common.bridge.world.border.WorldBorderBridge;
import net.minecraft.network.play.server.SWorldBorderPacket;
import net.minecraft.world.border.WorldBorder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SWorldBorderPacket.class)
public class SWorldBorderPacketMixin {

    // @formatter:off
    @Shadow private double centerX;
    @Shadow private double centerZ;
    // @formatter:on

    @Inject(method = "<init>(Lnet/minecraft/world/border/WorldBorder;Lnet/minecraft/network/play/server/SWorldBorderPacket$Action;)V", at = @At("RETURN"))
    private void arclight$nether(WorldBorder border, SWorldBorderPacket.Action actionIn, CallbackInfo ci) {
        this.centerX = border.getCenterX() * (((WorldBorderBridge) border).bridge$getWorld().getDimensionType().getCoordinateScale());
        this.centerZ = border.getCenterZ() * (((WorldBorderBridge) border).bridge$getWorld().getDimensionType().getCoordinateScale());
    }
}
