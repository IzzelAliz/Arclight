package io.izzel.arclight.common.mixin.core.world.storage;

import io.izzel.arclight.common.bridge.core.world.storage.LevelStorageSourceBridge;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.validation.ContentValidationException;
import net.minecraft.world.level.validation.DirectoryValidator;
import net.minecraft.world.level.validation.ForbiddenSymlinkInfo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@Mixin(LevelStorageSource.class)
public abstract class LevelStorageSourceMixin implements LevelStorageSourceBridge {

    // pdc implemented as WorldSavedData

    // @formatter:off
    @Shadow public abstract LevelStorageSource.LevelStorageAccess createAccess(String saveName) throws IOException;
    @Shadow protected abstract Path getLevelPath(String p_289974_);
    @Shadow @Final private DirectoryValidator worldDirValidator;
    // @formatter:on

    public LevelStorageSource.LevelStorageAccess validateAndCreateAccess(String s, ResourceKey<LevelStem> dimensionType) throws IOException, ContentValidationException {
        Path path = this.getLevelPath(s);
        List<ForbiddenSymlinkInfo> list = this.worldDirValidator.validateDirectory(path, true);
        if (!list.isEmpty()) {
            throw new ContentValidationException(path, list);
        } else {
            LevelStorageSource.LevelStorageAccess save = createAccess(s);
            ((LevelStorageAccessBridge) save).bridge$setDimType(dimensionType);
            return save;
        }
    }

    public LevelStorageSource.LevelStorageAccess createAccess(String saveName, ResourceKey<LevelStem> world) throws IOException {
        LevelStorageSource.LevelStorageAccess save = createAccess(saveName);
        ((LevelStorageAccessBridge) save).bridge$setDimType(world);
        return save;
    }

    @Override
    public LevelStorageSource.LevelStorageAccess bridge$getLevelSave(String saveName, ResourceKey<LevelStem> world) throws IOException {
        return createAccess(saveName, world);
    }
}
