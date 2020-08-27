package io.izzel.arclight.common.bridge.world;

import net.minecraft.entity.Entity;
import net.minecraft.util.Direction;
import net.minecraft.util.TeleportationRepositioner;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

public interface TeleporterBridge {

    Optional<TeleportationRepositioner.Result> bridge$findPortal(BlockPos pos, int searchRadius);

    Optional<TeleportationRepositioner.Result> bridge$createPortal(BlockPos pos, Direction.Axis axis, Entity entity, int createRadius);
}
