package io.izzel.arclight.common.bridge.world;

import net.minecraft.block.pattern.BlockPattern;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;

public interface TeleporterBridge {

    boolean bridge$makePortal(Entity entityIn, BlockPos pos, int createRadius);

    BlockPattern.PortalInfo bridge$placeInPortal(Entity p_222268_1_, BlockPos pos, float p_222268_2_, int searchRadius, boolean searchOnly);
}
