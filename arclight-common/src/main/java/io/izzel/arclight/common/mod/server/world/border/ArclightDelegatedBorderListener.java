package io.izzel.arclight.common.mod.server.world.border;

import net.minecraft.world.level.border.BorderChangeListener;
import net.minecraft.world.level.border.WorldBorder;

public class ArclightDelegatedBorderListener implements BorderChangeListener {

    public static boolean isEnabled() {
        // return ArclightConfig.spec().getCompat().isAssociateWorldBorder();
        return true;
    }

    private final BorderChangeListener delegate;

    public ArclightDelegatedBorderListener(BorderChangeListener delegate) {
        this.delegate = delegate;
    }

    @Override
    public void onSetSize(WorldBorder border, double newSize) {
        if (!isEnabled()) { return; }
        delegate.onSetSize(border, newSize);
    }

    @Override
    public void onSetCenter(WorldBorder border, double x, double z) {
        if (!isEnabled()) { return; }
        delegate.onSetCenter(border, x, z);
    }

    @Override
    public void onLerpSize(WorldBorder border, double fromSize, double targetSize, long ticks, long gameTime) {
        if (!isEnabled()) { return; }
        delegate.onLerpSize(border, fromSize, targetSize, ticks, gameTime);
    }

    @Override
    public void onSetWarningTime(WorldBorder border, int time) {
        if (!isEnabled()) { return; }
        delegate.onSetWarningTime(border, time);
    }

    @Override
    public void onSetWarningBlocks(WorldBorder border, int blocks) {
        if (!isEnabled()) { return; }
        delegate.onSetWarningBlocks(border, blocks);
    }

    @Override
    public void onSetDamagePerBlock(WorldBorder border, double damagePerBlock) {
        if (!isEnabled()) { return; }
        delegate.onSetDamagePerBlock(border, damagePerBlock);
    }

    @Override
    public void onSetSafeZone(WorldBorder border, double safeZone) {
        if (!isEnabled()) { return; }
        delegate.onSetSafeZone(border, safeZone);
    }
}
