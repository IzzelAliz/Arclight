package io.izzel.arclight.common.bridge.world;

import java.util.Optional;
import net.minecraft.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;

public interface TeleporterBridge {

    Optional<BlockUtil.FoundRectangle> bridge$findPortal(BlockPos pos, int searchRadius);

    Optional<BlockUtil.FoundRectangle> bridge$createPortal(BlockPos pos, Direction.Axis axis, Entity entity, int createRadius);
}
