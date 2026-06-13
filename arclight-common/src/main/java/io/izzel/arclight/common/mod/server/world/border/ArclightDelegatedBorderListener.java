package io.izzel.arclight.common.mod.server.world.border;

import net.minecraft.world.level.border.BorderChangeListener;
import net.minecraft.world.level.border.WorldBorder;

public class ArclightDelegatedBorderListener implements BorderChangeListener {

    public static boolean isEnabled() {
        return true;
    }

    private final WorldBorder border;
    private final BorderChangeListener delegate;

    public ArclightDelegatedBorderListener(WorldBorder border, BorderChangeListener delegate) {
        this.border = border;
        this.delegate = delegate;
    }

    @Override
    public void onSetSize(WorldBorder worldBorder, double size) {
        if (!isEnabled()) {
            return;
        }
        delegate.onSetSize(worldBorder, size);
    }

    @Override
    public void onSetCenter(WorldBorder worldBorder, double x, double z) {
        if (!isEnabled()) {
            return;
        }
        delegate.onSetCenter(worldBorder, x, z);
    }

    @Override
    public void onLerpSize(WorldBorder worldBorder, double from, double to, long time, long startTime) {
        if (!isEnabled()) {
            return;
        }
        delegate.onLerpSize(worldBorder, from, to, time, startTime);
    }

    @Override
    public void onSetWarningTime(WorldBorder worldBorder, int time) {
        if (!isEnabled()) {
            return;
        }
        delegate.onSetWarningTime(worldBorder, time);
    }

    @Override
    public void onSetWarningBlocks(WorldBorder worldBorder, int distance) {
        if (!isEnabled()) {
            return;
        }
        delegate.onSetWarningBlocks(worldBorder, distance);
    }

    @Override
    public void onSetDamagePerBlock(WorldBorder worldBorder, double damage) {
        if (!isEnabled()) {
            return;
        }
        delegate.onSetDamagePerBlock(worldBorder, damage);
    }

    @Override
    public void onSetSafeZone(WorldBorder worldBorder, double safeZone) {
        if (!isEnabled()) {
            return;
        }
        delegate.onSetSafeZone(worldBorder, safeZone);
    }
}
