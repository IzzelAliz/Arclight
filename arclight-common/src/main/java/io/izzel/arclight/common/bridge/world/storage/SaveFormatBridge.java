package io.izzel.arclight.common.bridge.world.storage;

import net.minecraft.util.RegistryKey;
import net.minecraft.world.Dimension;
import net.minecraft.world.storage.SaveFormat;

import java.io.IOException;

public interface SaveFormatBridge {

    SaveFormat.LevelSave bridge$getLevelSave(String saveName, RegistryKey<Dimension> world) throws IOException;

    interface LevelSaveBridge {

        void bridge$setDimType(RegistryKey<Dimension> typeKey);
    }
}
