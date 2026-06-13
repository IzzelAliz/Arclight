package io.izzel.arclight.common.mod.util;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

public final class ArclightTeleportHelper {

    private ArclightTeleportHelper() {
    }

    public static TeleportTransition withLocation(TeleportTransition from, ServerLevel level, Vec3 position, float yRot, float xRot) {
        return new TeleportTransition(
            level,
            position,
            from.deltaMovement(),
            yRot,
            xRot,
            from.missingRespawnBlock(),
            from.asPassenger(),
            from.relatives(),
            from.postTeleportTransition()
        );
    }
}
