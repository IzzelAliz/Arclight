package io.izzel.arclight.mixin.core.world.chunk.storage;

import io.izzel.arclight.bridge.world.chunk.storage.RegionFileCacheBridge;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.storage.RegionFile;
import net.minecraft.world.chunk.storage.RegionFileCache;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.io.File;
import java.io.IOException;

@Mixin(RegionFileCache.class)
public class RegionFileCacheMixin implements RegionFileCacheBridge {

    // @formatter:off
    @Shadow @Final public Long2ObjectLinkedOpenHashMap<RegionFile> cache;
    @Shadow @Final private File folder;
    // @formatter:on

    private RegionFile loadFile(ChunkPos pos, boolean existsOnly) throws IOException {
        long i = ChunkPos.asLong(pos.getRegionCoordX(), pos.getRegionCoordZ());
        RegionFile regionfile = this.cache.getAndMoveToFirst(i);
        if (regionfile != null) {
            return regionfile;
        } else {
            if (this.cache.size() >= 256) {
                this.cache.removeLast().close();
            }

            if (!this.folder.exists()) {
                this.folder.mkdirs();
            }

            File file1 = new File(this.folder, "r." + pos.getRegionCoordX() + "." + pos.getRegionCoordZ() + ".mca");
            if (existsOnly && !file1.exists()) return null;

            RegionFile regionfile1 = new RegionFile(file1);
            this.cache.putAndMoveToFirst(i, regionfile1);
            return regionfile1;
        }
    }

    public boolean chunkExists(ChunkPos pos) throws IOException {
        RegionFile regionFile = loadFile(pos, true);
        return regionFile != null && regionFile.contains(pos);
    }

    @Override
    public boolean bridge$chunkExists(ChunkPos pos) throws IOException {
        return chunkExists(pos);
    }
}
