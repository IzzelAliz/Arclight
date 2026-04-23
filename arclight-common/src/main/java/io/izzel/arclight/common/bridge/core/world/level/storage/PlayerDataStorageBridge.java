package io.izzel.arclight.common.bridge.core.world.level.storage;

import java.io.File;
import net.minecraft.nbt.CompoundTag;

public interface PlayerDataStorageBridge {

    File bridge$getPlayerDir();

    CompoundTag bridge$getPlayerData(String uuid);
}
