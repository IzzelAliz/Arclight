package io.izzel.arclight.common.mixin.core.world.chunk.storage;

import io.izzel.arclight.common.bridge.world.dimension.DimensionTypeBridge;
import net.minecraft.world.chunk.storage.ChunkLoader;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ChunkLoader.class)
public class ChunkLoaderMixin {

    @ModifyArg(method = "updateChunkData", index = 0, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/gen/feature/structure/LegacyStructureDataUtil;func_215130_a(Lnet/minecraft/world/dimension/DimensionType;Lnet/minecraft/world/storage/DimensionSavedDataManager;)Lnet/minecraft/world/gen/feature/structure/LegacyStructureDataUtil;"))
    private DimensionType arclight$dimType(DimensionType dim) {
        return ((DimensionTypeBridge) dim).bridge$getType();
    }
}
