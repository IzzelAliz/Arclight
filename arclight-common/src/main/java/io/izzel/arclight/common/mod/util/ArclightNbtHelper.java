package io.izzel.arclight.common.mod.util;

import io.izzel.arclight.common.mixin.core.world.level.storage.TagValueInputAccessor;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.UUID;

public final class ArclightNbtHelper {

    private ArclightNbtHelper() {
    }

    public static ValueInput wrapInput(CompoundTag tag, HolderLookup.Provider registryAccess) {
        return TagValueInput.create(ProblemReporter.DISCARDING, registryAccess, tag);
    }

    public static CompoundTag asCompound(ValueInput input) {
        if (input instanceof TagValueInput tagValueInput) {
            return ((TagValueInputAccessor) (Object) tagValueInput).arclight$getInput();
        }
        throw new IllegalArgumentException("ValueInput is not backed by CompoundTag");
    }

    public static int getInt(ValueInput input, String key) {
        return input.getIntOr(key, 0);
    }

    public static long getLong(ValueInput input, String key) {
        return input.getLongOr(key, 0L);
    }

    public static String getString(ValueInput input, String key) {
        return input.getStringOr(key, "");
    }

    public static boolean getBoolean(ValueInput input, String key) {
        return input.getBooleanOr(key, false);
    }

    public static boolean getBoolean(ValueInput input, String key, boolean defaultValue) {
        return input.getBooleanOr(key, defaultValue);
    }

    public static boolean contains(ValueInput input, String key) {
        if (input instanceof TagValueInput tagValueInput) {
            return ((TagValueInputAccessor) (Object) tagValueInput).arclight$getInput().contains(key);
        }
        return false;
    }

    public static void putInt(ValueOutput output, String key, int value) {
        output.putInt(key, value);
    }

    public static void putBoolean(ValueOutput output, String key, boolean value) {
        output.putBoolean(key, value);
    }

    public static void putString(ValueOutput output, String key, String value) {
        output.putString(key, value);
    }

    public static void putLong(ValueOutput output, String key, long value) {
        output.putLong(key, value);
    }

    public static UUID getUuid(ValueInput input, String mostKey, String leastKey) {
        if (input.getLong(mostKey).isPresent() && input.getLong(leastKey).isPresent()) {
            return new UUID(input.getLongOr(mostKey, 0L), input.getLongOr(leastKey, 0L));
        }
        return null;
    }
}
