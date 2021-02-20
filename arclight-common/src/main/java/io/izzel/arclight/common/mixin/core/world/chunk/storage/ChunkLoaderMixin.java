package io.izzel.arclight.common.mixin.core.world.chunk.storage;

import com.google.common.collect.ImmutableList;
import net.minecraft.util.RegistryKey;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.chunk.storage.ChunkLoader;
import net.minecraft.world.gen.feature.structure.LegacyStructureDataUtil;
import net.minecraft.world.storage.DimensionSavedDataManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import javax.annotation.Nullable;
import java.util.List;

@Mixin(ChunkLoader.class)
public abstract class ChunkLoaderMixin {

    @Redirect(method = "func_235968_a_", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/gen/feature/structure/LegacyStructureDataUtil;func_236992_a_(Lnet/minecraft/util/RegistryKey;Lnet/minecraft/world/storage/DimensionSavedDataManager;)Lnet/minecraft/world/gen/feature/structure/LegacyStructureDataUtil;"))
    private LegacyStructureDataUtil arclight$legacyData(RegistryKey<World> p_236992_0_, DimensionSavedDataManager p_236992_1_) {
        return legacyDataOf(p_236992_0_, p_236992_1_);
    }

    /**
     * From {@link LegacyStructureDataUtil#func_236992_a_(RegistryKey, DimensionSavedDataManager)}
     */
    private static LegacyStructureDataUtil legacyDataOf(RegistryKey<?> typeKey, @Nullable DimensionSavedDataManager dataManager) {
        if (typeKey == DimensionType.OVERWORLD || typeKey == World.OVERWORLD) {
            return new LegacyStructureDataUtil(dataManager, ImmutableList.of("Monument", "Stronghold", "Village", "Mineshaft", "Temple", "Mansion"), ImmutableList.of("Village", "Mineshaft", "Mansion", "Igloo", "Desert_Pyramid", "Jungle_Pyramid", "Swamp_Hut", "Stronghold", "Monument"));
        } else if (typeKey == DimensionType.THE_NETHER || typeKey == World.THE_NETHER) {
            List<String> list1 = ImmutableList.of("Fortress");
            return new LegacyStructureDataUtil(dataManager, list1, list1);
        } else if (typeKey == DimensionType.THE_END || typeKey == World.THE_END) {
            List<String> list = ImmutableList.of("EndCity");
            return new LegacyStructureDataUtil(dataManager, list, list);
        } else {
            throw new RuntimeException(String.format("Unknown dimension type : %s", typeKey));
        }
    }
}
