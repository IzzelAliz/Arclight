package io.izzel.arclight.common.bridge.world;

import net.minecraft.entity.Entity;
import net.minecraft.util.TeleportationRepositioner;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

public interface TeleporterBridge {

    boolean bridge$makePortal(Entity entityIn, BlockPos pos, int createRadius);

    Optional<TeleportationRepositioner.Result> bridge$findPortal(BlockPos pos, int searchRadius);
}
