package io.izzel.arclight.common.mixin.core.network.protocol.game;

import io.izzel.arclight.common.bridge.core.world.border.WorldBorderBridge;
import net.minecraft.network.protocol.game.ClientboundSetBorderCenterPacket;
import net.minecraft.world.level.border.WorldBorder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientboundSetBorderCenterPacket.class)
public class SWorldBorderPacketMixin {

    // @formatter:off
    @Shadow @Final @Mutable private double newCenterX;
    @Shadow @Final @Mutable private double newCenterZ;
    // @formatter:on

    @Inject(method = "<init>(Lnet/minecraft/world/level/border/WorldBorder;)V", at = @At("RETURN"))
    private void arclight$nether(WorldBorder border, CallbackInfo ci) {
        this.newCenterX = border.getCenterX() * (((WorldBorderBridge) border).bridge$getWorld().dimensionType().coordinateScale());
        this.newCenterZ = border.getCenterZ() * (((WorldBorderBridge) border).bridge$getWorld().dimensionType().coordinateScale());
    }
}
