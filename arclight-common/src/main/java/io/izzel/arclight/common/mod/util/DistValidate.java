package io.izzel.arclight.common.mod.util;

import io.izzel.arclight.i18n.ArclightConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;

public class DistValidate {

    public static boolean isValid(UseOnContext context) {
        return context != null && isValid(context.getLevel());
    }

    public static boolean isValid(LevelAccessor level) {
        return level != null
            && !level.isClientSide()
            && isLogicWorld(level);
    }

    public static boolean isValid(BlockGetter getter) {
        return getter instanceof LevelAccessor level && isValid(level);
    }

    private static boolean isLogicWorld(LevelAccessor level) {
        var cl = level.getClass();
        return cl == ServerLevel.class || cl == WorldGenRegion.class
            || ArclightConfig.spec().getCompat().getExtraLogicWorlds().contains(cl.getName());
    }
}
