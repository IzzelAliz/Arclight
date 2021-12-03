package io.izzel.arclight.common.mixin.core.world.storage;

import io.izzel.arclight.common.bridge.core.world.storage.LevelStorageSourceBridge;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.io.IOException;

@Mixin(LevelStorageSource.class)
public abstract class LevelStorageSourceMixin implements LevelStorageSourceBridge {

    // @formatter:off
    @Shadow public abstract LevelStorageSource.LevelStorageAccess createAccess(String saveName) throws IOException;
    // @formatter:on

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
