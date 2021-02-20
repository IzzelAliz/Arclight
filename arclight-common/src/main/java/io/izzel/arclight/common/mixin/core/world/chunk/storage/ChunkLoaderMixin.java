package io.izzel.arclight.common.mixin.core.world.chunk.storage;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.DataFixer;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.SharedConstants;
import net.minecraft.util.datafix.DefaultTypeReferences;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.chunk.storage.ChunkLoader;
import net.minecraft.world.gen.feature.structure.LegacyStructureDataUtil;
import net.minecraft.world.storage.DimensionSavedDataManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Supplier;

@Mixin(ChunkLoader.class)
public abstract class ChunkLoaderMixin {

    // @formatter:off
    @Shadow public static int getDataVersion(CompoundNBT compound) { return 0; }
    @Shadow @Final protected DataFixer dataFixer;
    @Shadow @Nullable private LegacyStructureDataUtil field_219167_a;
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public CompoundNBT func_235968_a_(RegistryKey<?> p_235968_1_, Supplier<DimensionSavedDataManager> p_235968_2_, CompoundNBT nbt) {
        int i = getDataVersion(nbt);
        if (i < 1493) {
            nbt = NBTUtil.update(this.dataFixer, DefaultTypeReferences.CHUNK, nbt, i, 1493);
            if (nbt.getCompound("Level").getBoolean("hasLegacyStructureData")) {
                if (this.field_219167_a == null) {
                    this.field_219167_a = legacyDataOf(p_235968_1_, p_235968_2_.get());
                }

                nbt = this.field_219167_a.func_212181_a(nbt);
            }
        }

        nbt = NBTUtil.update(this.dataFixer, DefaultTypeReferences.CHUNK, nbt, Math.max(1493, i));
        if (i < SharedConstants.getVersion().getWorldVersion()) {
            nbt.putInt("DataVersion", SharedConstants.getVersion().getWorldVersion());
        }

        return nbt;
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
