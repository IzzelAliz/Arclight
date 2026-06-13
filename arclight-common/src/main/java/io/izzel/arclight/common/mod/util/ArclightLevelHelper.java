package io.izzel.arclight.common.mod.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelData;

public final class ArclightLevelHelper {

    private ArclightLevelHelper() {
    }

    public static long getDayTime(Level level) {
        return level.getDefaultClockTime();
    }

    public static void setDayTime(ServerLevel level, long time) {
        level.dimensionType().defaultClock().ifPresent(clock -> level.clockManager().setTotalTicks(clock, time));
    }

    public static BlockPos getSharedSpawnPos(ServerLevel level) {
        return level.getRespawnData().pos();
    }

    public static float getSharedSpawnAngle(ServerLevel level) {
        return level.getRespawnData().yaw();
    }

    public static LevelData.RespawnData getSharedSpawnRespawnData(ServerLevel level) {
        var respawn = level.getRespawnData();
        return LevelData.RespawnData.of(level.dimension(), respawn.pos(), respawn.yaw(), respawn.pitch());
    }

    public static void setDefaultSpawnPos(ServerLevel level, BlockPos pos, float angle) {
        var respawn = level.getRespawnData();
        level.setRespawnData(LevelData.RespawnData.of(level.dimension(), pos, angle, respawn.pitch()));
    }

    public static Holder<WorldClock> defaultClock(ServerLevel level) {
        return level.dimensionType().defaultClock().orElseThrow();
    }
}
