package io.izzel.arclight.common.mixin.core.world.storage;

import io.izzel.arclight.common.bridge.core.world.storage.LevelStorageSourceBridge;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.file.Path;

@Mixin(LevelStorageSource.LevelStorageAccess.class)
public class LevelStorageSource_LevelStorageAccessMixin implements LevelStorageSourceBridge.LevelStorageAccessBridge {

    @Shadow @Final LevelStorageSource.LevelDirectory levelDirectory;

    public ResourceKey<LevelStem> dimensionType;

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

    @Override
    public ResourceKey<LevelStem> bridge$getTypeKey() {
        return this.dimensionType;
    }

    @Inject(method = "getDimensionPath", cancellable = true, at = @At("HEAD"))
    private void arclight$useActualType(ResourceKey<Level> dimensionKey, CallbackInfoReturnable<Path> cir) {
        if (dimensionType == LevelStem.OVERWORLD) {
            cir.setReturnValue(this.levelDirectory.path());
        } else if (dimensionType == LevelStem.NETHER) {
            cir.setReturnValue(this.levelDirectory.path().resolve("DIM-1"));
        } else if (dimensionType == LevelStem.END) {
            cir.setReturnValue(this.levelDirectory.path().resolve("DIM1"));
        }
    }
}
