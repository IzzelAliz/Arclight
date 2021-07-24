package io.izzel.arclight.common.bridge.world.storage;

import java.io.IOException;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.LevelStorageSource;

public interface SaveFormatBridge {

    LevelStorageSource.LevelStorageAccess bridge$getLevelSave(String saveName, ResourceKey<LevelStem> world) throws IOException;

    interface LevelSaveBridge {

        void bridge$setDimType(ResourceKey<LevelStem> typeKey);
    }
}
