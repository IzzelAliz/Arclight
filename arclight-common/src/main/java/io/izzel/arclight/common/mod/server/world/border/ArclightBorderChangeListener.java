package io.izzel.arclight.common.mod.server.world.border;

import io.izzel.arclight.common.bridge.core.world.level.border.WorldBorderBridge;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.level.border.BorderChangeListener;
import net.minecraft.world.level.border.WorldBorder;

import java.util.function.Function;

public class ArclightBorderChangeListener implements BorderChangeListener {

    public static final ArclightBorderChangeListener INSTANCE = new ArclightBorderChangeListener();

    public static BorderChangeListener typed() {
        return INSTANCE;
    }

    @Override
    public void onBorderSizeSet(WorldBorder border, double d) {
        arclight$broadcastToDimension(border, ClientboundSetBorderSizePacket::new);
    }

    @Override
    public void onBorderSizeLerping(WorldBorder border, double d, double e, long l) {
        arclight$broadcastToDimension(border, ClientboundSetBorderLerpSizePacket::new);
    }

    @Override
    public void onBorderCenterSet(WorldBorder border, double d, double e) {
        arclight$broadcastToDimension(border, ClientboundSetBorderCenterPacket::new);
    }

    @Override
    public void onBorderSetWarningTime(WorldBorder border, int i) {
        arclight$broadcastToDimension(border, ClientboundSetBorderWarningDelayPacket::new);
    }

    @Override
    public void onBorderSetWarningBlocks(WorldBorder border, int i) {
        arclight$broadcastToDimension(border, ClientboundSetBorderWarningDistancePacket::new);
    }

    @Override
    public void onBorderSetDamagePerBlock(WorldBorder border, double d) {

    }

    @Override
    public void onBorderSetDamageSafeZOne(WorldBorder border, double d) {

    }

    private void arclight$broadcastToDimension(WorldBorder border, Function<WorldBorder, Packet<?>> packet) {
        final var level = ((WorldBorderBridge) border).bridge$getWorld();
        level.getServer().getPlayerList().broadcastAll(packet.apply(border), level.dimension());
    }
}
