package io.izzel.arclight.common.mixin.core.world.storage;

import io.izzel.arclight.common.bridge.world.storage.SaveFormatBridge;
import net.minecraft.util.RegistryKey;
import net.minecraft.world.Dimension;
import net.minecraft.world.storage.SaveFormat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.io.IOException;

@Mixin(SaveFormat.class)
public abstract class SaveFormatMixin implements SaveFormatBridge {

    // @formatter:off
    @Shadow public abstract SaveFormat.LevelSave getLevelSave(String saveName) throws IOException;
    // @formatter:on

    public SaveFormat.LevelSave getLevelSave(String saveName, RegistryKey<Dimension> world) throws IOException {
        SaveFormat.LevelSave save = getLevelSave(saveName);
        ((LevelSaveBridge) save).bridge$setDimType(world);
        return save;
    }

    // mock
    public SaveFormat.LevelSave c(String saveName, RegistryKey<Dimension> world) throws IOException {
        return getLevelSave(saveName, world);
    }

    @Override
    public SaveFormat.LevelSave bridge$getLevelSave(String saveName, RegistryKey<Dimension> world) throws IOException {
        return getLevelSave(saveName, world);
    }
}
