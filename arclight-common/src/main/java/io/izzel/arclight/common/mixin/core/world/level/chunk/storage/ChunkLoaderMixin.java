package io.izzel.arclight.common.mixin.core.world.level.chunk.storage;

import com.google.common.collect.ImmutableList;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.storage.ChunkStorage;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.structure.LegacyStructureDataHandler;
import net.minecraft.world.level.storage.DimensionDataStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import javax.annotation.Nullable;
import java.util.List;

@Mixin(ChunkStorage.class)
public abstract class ChunkLoaderMixin {

    @Redirect(method = "upgradeChunkTag", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/structure/LegacyStructureDataHandler;getLegacyStructureHandler(Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/world/level/storage/DimensionDataStorage;)Lnet/minecraft/world/level/levelgen/structure/LegacyStructureDataHandler;"))
    private LegacyStructureDataHandler arclight$legacyData(ResourceKey<Level> p_236992_0_, DimensionDataStorage p_236992_1_) {
        return legacyDataOf(p_236992_0_, p_236992_1_);
    }

    /**
     * From {@link LegacyStructureDataHandler#getLegacyStructureHandler(ResourceKey, DimensionDataStorage)}
     */
    private static LegacyStructureDataHandler legacyDataOf(ResourceKey<?> typeKey, @Nullable DimensionDataStorage dataManager) {
        if (typeKey == LevelStem.OVERWORLD || typeKey == Level.OVERWORLD) {
            return new LegacyStructureDataHandler(dataManager, ImmutableList.of("Monument", "Stronghold", "Village", "Mineshaft", "Temple", "Mansion"), ImmutableList.of("Village", "Mineshaft", "Mansion", "Igloo", "Desert_Pyramid", "Jungle_Pyramid", "Swamp_Hut", "Stronghold", "Monument"));
        } else if (typeKey == LevelStem.NETHER || typeKey == Level.NETHER) {
            List<String> list1 = ImmutableList.of("Fortress");
            return new LegacyStructureDataHandler(dataManager, list1, list1);
        } else if (typeKey == LevelStem.END || typeKey == Level.END) {
            List<String> list = ImmutableList.of("EndCity");
            return new LegacyStructureDataHandler(dataManager, list, list);
        } else {
            throw new RuntimeException(String.format("Unknown dimension type : %s", typeKey));
        }
    }
}
