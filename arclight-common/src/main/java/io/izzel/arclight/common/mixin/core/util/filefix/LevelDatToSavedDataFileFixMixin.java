package io.izzel.arclight.common.mixin.core.util.filefix;

import com.mojang.serialization.Dynamic;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.util.filefix.fixes.LevelDatToSavedDataFileFix;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LevelDatToSavedDataFileFix.class)
public abstract class LevelDatToSavedDataFileFixMixin {

    private static final int ARCLIGHT$WARNING_TIME_TICKS_VERSION = 4657;

    @ModifyVariable(
        method = "extractWorldBorderToFiles(Lnet/minecraft/util/filefix/access/FileAccess;Lnet/minecraft/util/filefix/access/FileAccess;Lnet/minecraft/util/filefix/access/FileAccess;Lcom/mojang/serialization/Dynamic;)Lcom/mojang/serialization/Dynamic;",
        at = @At("HEAD"), argsOnly = true, index = 3, require = 1)
    private static Dynamic<?> arclight$scaleLegacyBorderWarningTime(Dynamic<?> levelData) {
        // LevelDat.read leaves DataVersion on the LEVEL root; the extracted border file
        // is stamped as current by SavedDataNbt.write, so migrate before extracting it.
        if (NbtUtils.getDataVersion(levelData) >= ARCLIGHT$WARNING_TIME_TICKS_VERSION) {
            return levelData;
        }

        if (levelData.get("world_border").result().isEmpty()) {
            return levelData;
        }

        return levelData.update("world_border", border -> border.update("warning_time",
            warningTime -> warningTime.createInt(warningTime.asInt(15) * 20)));
    }
}
