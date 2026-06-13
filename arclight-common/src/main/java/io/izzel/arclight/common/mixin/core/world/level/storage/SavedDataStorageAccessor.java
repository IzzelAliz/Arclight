package io.izzel.arclight.common.mixin.core.world.level.storage;

import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.SavedDataStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
import java.util.Optional;

@Mixin(SavedDataStorage.class)
public interface SavedDataStorageAccessor {

    @Accessor
    Map<SavedDataType<?>, Optional<SavedData>> cache();
}
