package io.izzel.arclight.common.mixin.core.world.level.chunk;

import io.izzel.arclight.common.bridge.core.world.chunk.ChunkAccessBridge;
import io.izzel.arclight.common.bridge.core.world.chunk.LevelChunkSectionBridge;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.FeatureAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import org.bukkit.craftbukkit.v.persistence.CraftPersistentDataContainer;
import org.bukkit.craftbukkit.v.persistence.CraftPersistentDataTypeRegistry;
import org.bukkit.persistence.PersistentDataContainer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(ChunkAccess.class)
public abstract class ChunkAccessMixin implements BlockGetter, BiomeManager.NoiseBiomeSource, FeatureAccess, ChunkAccessBridge {

    // @formatter:off
    @Shadow public abstract void setUnsaved(boolean p_62094_);
    @Shadow public abstract int getMinBuildHeight();
    @Shadow public abstract int getHeight();
    @Shadow public boolean isUnsaved() { return false; }
    @Shadow @Final protected LevelChunkSection[] sections;
    @Shadow @Final protected Map<BlockPos, CompoundTag> pendingBlockEntities;
    @Shadow @Final protected ChunkPos chunkPos;
    // @formatter:on


    private static final CraftPersistentDataTypeRegistry DATA_TYPE_REGISTRY = new CraftPersistentDataTypeRegistry();
    public CraftPersistentDataContainer persistentDataContainer = new CraftPersistentDataContainer(DATA_TYPE_REGISTRY);
    public Registry<Biome> biomeRegistry;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void arclight$init(ChunkPos p_187621_, UpgradeData p_187622_, LevelHeightAccessor p_187623_, Registry<Biome> registry, long p_187625_, LevelChunkSection[] p_187626_, BlendingData p_187627_, CallbackInfo ci) {
        this.biomeRegistry = registry;
        this.persistentDataContainer.setCallback(() -> this.setUnsaved(true));
    }

    @Override
    public PersistentDataContainer bridge$getPersistentDataContainer() {
        return this.persistentDataContainer;
    }

    public void setBiome(int i, int j, int k, Biome biome) {
        try {
            int l = QuartPos.fromBlock(this.getMinBuildHeight());
            int i1 = l + QuartPos.fromBlock(this.getHeight()) - 1;
            int j1 = Mth.clamp(j, l, i1);
            int k1 = this.getSectionIndex(QuartPos.toBlock(j1));

            ((LevelChunkSectionBridge) this.sections[k1]).bridge$setBiome(i & 3, j1 & 3, k & 3, biome);
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Setting biome");
            CrashReportCategory crashreportsystemdetails = crashreport.addCategory("Biome being set");

            crashreportsystemdetails.setDetail("Location", () -> {
                return CrashReportCategory.formatLocation(this, i, j, k);
            });
            throw new ReportedException(crashreport);
        }
    }
}
