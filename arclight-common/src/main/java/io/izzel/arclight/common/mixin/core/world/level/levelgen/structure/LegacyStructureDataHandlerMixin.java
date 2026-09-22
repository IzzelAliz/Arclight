package io.izzel.arclight.common.mixin.core.world.level.levelgen.structure;

import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.levelgen.structure.LegacyStructureDataHandler;
import net.minecraft.world.level.levelgen.structure.StructureFeatureIndexSavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;

@Mixin(LegacyStructureDataHandler.class)
public abstract class LegacyStructureDataHandlerMixin {

    @Shadow @Final @Mutable private boolean hasLegacyData;
    @Shadow @Final private Map<String, Long2ObjectMap<CompoundTag>> dataMap;
    @Shadow @Final private Map<String, StructureFeatureIndexSavedData> indexMap;
    @Shadow @Final private List<String> legacyKeys;
    @Shadow @Final private List<String> currentKeys;

    /**
     * Repair only state made internally inconsistent by the vanilla body:
     * it computes hasLegacyData before cache population and gives a newly
     * populated empty index to indexMap instead of the storage-owned instance.
     * This deliberately keeps the supplied DimensionDataStorage shared.
     */
    @Inject(method = "populateCaches", at = @At("TAIL"))
    private void arclight$repairPopulatedLegacyState(DimensionDataStorage storage, CallbackInfo ci) {
        for (String legacyKey : this.legacyKeys) {
            StructureFeatureIndexSavedData active = this.indexMap.get(legacyKey);
            if (active == null) {
                continue;
            }
            StructureFeatureIndexSavedData persisted = storage.computeIfAbsent(
                StructureFeatureIndexSavedData.type(legacyKey + "_index")
            );
            if (active == persisted || !persisted.getAll().isEmpty()) {
                continue;
            }
            for (LongIterator iterator = active.getAll().iterator(); iterator.hasNext();) {
                persisted.addIndex(iterator.nextLong());
            }
            this.indexMap.put(legacyKey, persisted);
        }
        this.hasLegacyData = this.currentKeys.stream().anyMatch(this.dataMap::containsKey);
    }
}
