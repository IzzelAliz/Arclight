package io.izzel.arclight.common.mixin.core.util.filefix;

import net.minecraft.util.filefix.FileFix;
import net.minecraft.util.filefix.access.FileRelation;
import net.minecraft.util.filefix.fixes.DimensionStorageFileFix;
import net.minecraft.util.filefix.operations.FileFixOperations;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;

@Mixin(DimensionStorageFileFix.class)
public abstract class DimensionStorageFileFixMixin {

    @Inject(method = "makeFixer()V", at = @At("HEAD"))
    private void arclight$worldPdc(CallbackInfo ci) {
        // FileFix calls makeFixer from its constructor; do not rely on mixin fields here.
        FileFix fix = (FileFix) (Object) this;
        fix.addFileFixOperation(FileFixOperations.applyInFolders(FileRelation.DIMENSIONS_DATA,
            List.of(FileFixOperations.move("bukkit_pdc.dat", "minecraft/bukkit_pdc.dat"))));
        fix.addFileFixOperation(FileFixOperations.groupMove(Map.of(
            "data", "dimensions/minecraft/overworld/data/minecraft",
            "DIM-1/data", "dimensions/minecraft/the_nether/data/minecraft",
            "DIM1/data", "dimensions/minecraft/the_end/data/minecraft"),
            List.of(FileFixOperations.moveSimple("bukkit_pdc.dat"))));
    }
}
