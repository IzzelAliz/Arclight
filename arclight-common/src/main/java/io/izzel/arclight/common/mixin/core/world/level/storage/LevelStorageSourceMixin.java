package io.izzel.arclight.common.mixin.core.world.level.storage;

import io.izzel.arclight.common.bridge.core.world.level.storage.LevelStorageSourceBridge;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.validation.ContentValidationException;
import net.minecraft.world.level.validation.DirectoryValidator;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.io.IOException;
import java.nio.file.Path;

@Mixin(LevelStorageSource.class)
public abstract class LevelStorageSourceMixin implements LevelStorageSourceBridge {

    // pdc implemented as WorldSavedData

    // @formatter:off
    @Shadow public abstract LevelStorageSource.LevelStorageAccess createAccess(String saveName) throws IOException;
    @Shadow protected abstract Path getLevelPath(String p_289974_);
    @Shadow @Final private DirectoryValidator worldDirValidator;
    // @formatter:on

    @Shadow
    public abstract LevelStorageSource.LevelStorageAccess validateAndCreateAccess(String saveName) throws IOException, ContentValidationException;

    public LevelStorageSource.LevelStorageAccess validateAndCreateAccess(String s, ResourceKey<LevelStem> dimensionType) throws IOException, ContentValidationException {
        final var result = this.validateAndCreateAccess(s);
        ((LevelStorageAccessBridge) result).bridge$setDimType(dimensionType);
        return result;
    }

    public LevelStorageSource.LevelStorageAccess createAccess(String saveName, ResourceKey<LevelStem> world) throws IOException {
        LevelStorageSource.LevelStorageAccess save = createAccess(saveName);
        ((LevelStorageAccessBridge) save).bridge$setDimType(world);
        return save;
    }

    @Override
    public LevelStorageSource.LevelStorageAccess arclight$validateAndCreateAccess(String saveName, ResourceKey<LevelStem> world) throws IOException, ContentValidationException {
        return validateAndCreateAccess(saveName, world);
    }
}
