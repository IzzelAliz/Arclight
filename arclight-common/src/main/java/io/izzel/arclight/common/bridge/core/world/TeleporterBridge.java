package io.izzel.arclight.common.bridge.core.world;

import net.minecraft.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.border.WorldBorder;

import java.util.Optional;

public interface TeleporterBridge {

    Optional<BlockUtil.FoundRectangle> bridge$findPortal(BlockPos pos, WorldBorder worldborder, int searchRadius);

    Optional<BlockUtil.FoundRectangle> bridge$createPortal(BlockPos pos, Direction.Axis axis, Entity entity, int createRadius);
}
