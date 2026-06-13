package io.izzel.arclight.common.mod.server.world.border;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.level.border.BorderChangeListener;
import net.minecraft.world.level.border.WorldBorder;
import io.izzel.arclight.common.bridge.core.world.level.border.WorldBorderBridge;

import java.util.function.Function;

public class ArclightBorderChangeListener implements BorderChangeListener {

    public static final ArclightBorderChangeListener INSTANCE = new ArclightBorderChangeListener();

    public static BorderChangeListener typed() {
        return INSTANCE;
    }

    @Override
    public void onSetSize(WorldBorder border, double size) {
        arclight$broadcastToDimension(border, ClientboundSetBorderSizePacket::new);
    }

    @Override
    public void onLerpSize(WorldBorder border, double from, double to, long time, long startTime) {
        arclight$broadcastToDimension(border, ClientboundSetBorderLerpSizePacket::new);
    }

    @Override
    public void onSetCenter(WorldBorder border, double x, double z) {
        arclight$broadcastToDimension(border, ClientboundSetBorderCenterPacket::new);
    }

    @Override
    public void onSetWarningTime(WorldBorder border, int time) {
        arclight$broadcastToDimension(border, ClientboundSetBorderWarningDelayPacket::new);
    }

    @Override
    public void onSetWarningBlocks(WorldBorder border, int distance) {
        arclight$broadcastToDimension(border, ClientboundSetBorderWarningDistancePacket::new);
    }

    @Override
    public void onSetDamagePerBlock(WorldBorder border, double damage) {
    }

    @Override
    public void onSetSafeZone(WorldBorder border, double safeZone) {
    }

    private void arclight$broadcastToDimension(WorldBorder border, Function<WorldBorder, Packet<?>> packet) {
        final var level = ((WorldBorderBridge) border).bridge$getWorld();
        level.getServer().getPlayerList().broadcastAll(packet.apply(border), level.dimension());
    }
}
