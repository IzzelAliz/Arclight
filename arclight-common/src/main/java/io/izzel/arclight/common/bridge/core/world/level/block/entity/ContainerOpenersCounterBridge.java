package io.izzel.arclight.common.bridge.core.world.level.block.entity;

public interface ContainerOpenersCounterBridge {
    boolean bridge$isOpened();
    void bridge$setOpened(boolean opened);
    void onAPIOpen(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state);
    void onAPIClose(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state);
    void openerAPICountChanged(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state, int oldCount, int newCount);
}
