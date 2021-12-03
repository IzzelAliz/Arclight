package io.izzel.arclight.common.bridge.core.world.storage;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.LevelStorageSource;

import java.io.IOException;

public interface LevelStorageSourceBridge {

    LevelStorageSource.LevelStorageAccess bridge$getLevelSave(String saveName, ResourceKey<LevelStem> world) throws IOException;

    interface LevelStorageAccessBridge {

        void bridge$setDimType(ResourceKey<LevelStem> typeKey);

        ResourceKey<LevelStem> bridge$getTypeKey();
    }
}
