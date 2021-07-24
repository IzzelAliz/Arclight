package io.izzel.arclight.common.mixin.core.world.storage;

import io.izzel.arclight.common.bridge.world.storage.SaveFormatBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.io.IOException;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.LevelStorageSource;

@Mixin(LevelStorageSource.class)
public abstract class SaveFormatMixin implements SaveFormatBridge {

    // @formatter:off
    @Shadow public abstract LevelStorageSource.LevelStorageAccess createAccess(String saveName) throws IOException;
    // @formatter:on

    public LevelStorageSource.LevelStorageAccess getLevelSave(String saveName, ResourceKey<LevelStem> world) throws IOException {
        LevelStorageSource.LevelStorageAccess save = createAccess(saveName);
        ((LevelSaveBridge) save).bridge$setDimType(world);
        return save;
    }

    // mock
    public LevelStorageSource.LevelStorageAccess c(String saveName, ResourceKey<LevelStem> world) throws IOException {
        return getLevelSave(saveName, world);
    }

    @Override
    public LevelStorageSource.LevelStorageAccess bridge$getLevelSave(String saveName, ResourceKey<LevelStem> world) throws IOException {
        return getLevelSave(saveName, world);
    }
}
