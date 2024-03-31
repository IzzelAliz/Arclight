package io.izzel.arclight.common.bridge.core.tileentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public interface EndGatewayBlockEntityBridge {

    void bridge$playerTeleportEvent(Level level, BlockPos pos, BlockState state, Entity entityIn, BlockPos dest);
}
