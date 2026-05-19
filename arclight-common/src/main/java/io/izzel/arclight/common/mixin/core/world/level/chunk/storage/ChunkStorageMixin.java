package io.izzel.arclight.common.mixin.core.world.level.chunk.storage;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.DataFixer;
import io.izzel.arclight.common.bridge.core.world.level.WorldBridge;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.structure.LegacyStructureDataHandler;
import net.minecraft.world.level.chunk.storage.LegacyTagFixer;
import net.minecraft.world.level.storage.DimensionDataStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Supplier;

@Mixin(ChunkMap.class)
public abstract class ChunkStorageMixin {

    /**
     * From {@link LegacyStructureDataHandler#getLegacyTagFixer(ResourceKey, Supplier, DataFixer)}
     */
    private static LegacyStructureDataHandler legacyDataOf(ResourceKey<?> typeKey, @Nullable DimensionDataStorage dataManager, DataFixer dataFixer) {
        if (typeKey == LevelStem.OVERWORLD || typeKey == Level.OVERWORLD) {
            return new LegacyStructureDataHandler(dataManager, ImmutableList.of("Monument", "Stronghold", "Village", "Mineshaft", "Temple", "Mansion"), ImmutableList.of("Village", "Mineshaft", "Mansion", "Igloo", "Desert_Pyramid", "Jungle_Pyramid", "Swamp_Hut", "Stronghold", "Monument"), dataFixer);
        } else if (typeKey == LevelStem.NETHER || typeKey == Level.NETHER) {
            List<String> list1 = ImmutableList.of("Fortress");
            return new LegacyStructureDataHandler(dataManager, list1, list1, dataFixer);
        } else if (typeKey == LevelStem.END || typeKey == Level.END) {
            List<String> list = ImmutableList.of("EndCity");
            return new LegacyStructureDataHandler(dataManager, list, list, dataFixer);
        } else {
            throw new RuntimeException(String.format("Unknown dimension type : %s", typeKey));
        }
    }
}
