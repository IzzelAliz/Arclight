package io.izzel.arclight.common.mixin.core.world.level.storage;

import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import com.mojang.datafixers.DataFixer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.storage.SavedDataStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SavedDataStorage.class)
public abstract class SavedDataStorageMixin {

    @Decorate(method = "readTagFromDisk(Ljava/nio/file/Path;Lnet/minecraft/util/datafix/DataFixTypes;I)Lnet/minecraft/nbt/CompoundTag;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/util/datafix/DataFixTypes;update(Lcom/mojang/datafixers/DataFixer;Lnet/minecraft/nbt/CompoundTag;II)Lnet/minecraft/nbt/CompoundTag;"),
        require = 1, expect = 1, allow = 1)
    private CompoundTag arclight$skipAbsentDataFix(DataFixTypes type, DataFixer fixer, CompoundTag tag, int oldVersion, int newVersion) throws Throwable {
        if (type == null) {
            // Data without a vanilla schema must retain the complete, unchanged root tag.
            return tag;
        }
        return (CompoundTag) DecorationOps.callsite().invoke(type, fixer, tag, oldVersion, newVersion);
    }
}
