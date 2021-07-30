package io.izzel.arclight.common.mixin.core.world.storage;

import io.izzel.arclight.common.bridge.core.world.storage.SaveFormatBridge;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;
import java.nio.file.Path;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.LevelStorageSource;

@Mixin(LevelStorageSource.LevelStorageAccess.class)
public class SaveFormat_LevelSaveMixin implements SaveFormatBridge.LevelSaveBridge {

    @Shadow @Final public Path levelPath;

    private ResourceKey<LevelStem> dimensionType;

    public void arclight$constructor(LevelStorageSource saveFormat, String saveName) {
        throw new RuntimeException();
    }

    public void arclight$constructor(LevelStorageSource saveFormat, String saveName, ResourceKey<LevelStem> dimensionType) {
        arclight$constructor(saveFormat, saveName);
        this.dimensionType = dimensionType;
    }

    @Override
    public void bridge$setDimType(ResourceKey<LevelStem> typeKey) {
        this.dimensionType = typeKey;
    }

    @Inject(method = "getDimensionPath", cancellable = true, at = @At("HEAD"))
    private void arclight$useActualType(ResourceKey<Level> dimensionKey, CallbackInfoReturnable<File> cir) {
        if (dimensionType == LevelStem.OVERWORLD) {
            cir.setReturnValue(this.levelPath.toFile());
        } else if (dimensionType == LevelStem.NETHER) {
            cir.setReturnValue(new File(this.levelPath.toFile(), "DIM-1"));
        } else if (dimensionType == LevelStem.END) {
            cir.setReturnValue(new File(this.levelPath.toFile(), "DIM1"));
        }
    }
}
