package io.izzel.arclight.common.mod.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class FrameBounds {
    private FrameBounds() {}

    public static AABB create(BlockPos pos, Direction direction, boolean hasMap) {
        Vec3 center = Vec3.atCenterOf(pos).relative(direction, -0.46875D);
        double width = hasMap ? 1.0D : 0.75D;
        Direction.Axis axis = direction.getAxis();
        return AABB.ofSize(center, axis == Direction.Axis.X ? 0.0625D : width,
            axis == Direction.Axis.Y ? 0.0625D : width, axis == Direction.Axis.Z ? 0.0625D : width);
    }
}
