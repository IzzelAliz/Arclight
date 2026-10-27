package io.izzel.arclight.common.bridge.core.world.level.block.entity;

public interface ConduitBlockEntityBridge {
    void bridge$updateTarget(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state, boolean active);
}
