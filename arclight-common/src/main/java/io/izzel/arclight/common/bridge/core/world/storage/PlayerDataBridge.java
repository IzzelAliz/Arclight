package io.izzel.arclight.common.bridge.core.world.storage;

import java.io.File;
import net.minecraft.nbt.CompoundTag;

public interface PlayerDataBridge {

    File bridge$getPlayerDir();

    CompoundTag bridge$getPlayerData(String uuid);
}
