package io.izzel.arclight.common.mixin.core.world.storage;

import io.izzel.arclight.common.bridge.core.world.level.storage.LevelStorageSourceBridge;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LevelStorageSource.LevelStorageAccess.class)
public class LevelStorageSource_LevelStorageAccessMixin implements LevelStorageSourceBridge.LevelStorageAccessBridge {

    public ResourceKey<LevelStem> dimensionType;

    @Override
    public void bridge$setDimType(ResourceKey<LevelStem> typeKey) {
        this.dimensionType = typeKey;
    }

    @Override
    public ResourceKey<LevelStem> bridge$getTypeKey() {
        return this.dimensionType;
    }

    @ModifyVariable(method = "getDimensionPath", argsOnly = true, ordinal = 0, at = @At("HEAD"))
    private ResourceKey<Level> arclight$useActualType(ResourceKey<Level> value) {
        final var type = dimensionType;
        return type == null ? value : ResourceKey.create(Registries.DIMENSION, type.location());
    }
}
