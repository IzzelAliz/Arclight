package io.izzel.arclight.common.mixin.core.world.level.chunk.storage;

import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.world.level.chunk.storage.RegionFile;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

@Mixin(RegionFile.class)
public abstract class RegionFileMixin {

    // @formatter:off
    @Shadow private static int getSectorNumber(int i) { return 0; }
    @Shadow @Final private FileChannel file;
    // @formatter:on

    /**
     * Preserve the legacy inline-255 reader while using a checked sector count.
     * The constructor variant must not preempt RegionFile's own invalid-offset
     * recovery: its native sectorNumber/out-of-file checks clear bad entries.
     */
    private int arclight$legacySectorCount(int offset, int nativeCount, boolean constructing) throws IOException {
        if (nativeCount != 255) {
            return nativeCount;
        }

        if (constructing) {
            int sectorNumber = getSectorNumber(offset);
            if (sectorNumber < 2 || sectorNumber * 4096L > this.file.size()) {
                return nativeCount;
            }
            try {
                return arclight$readLegacySectorCount(offset);
            } catch (IOException damagedChunkHeader) {
                // Keep one damaged inline entry from aborting RegionFile open.
                // The reader hook reports the malformed entry when that chunk is read.
                return nativeCount;
            }
        }

        return arclight$readLegacySectorCount(offset);
    }

    private int arclight$readLegacySectorCount(int offset) throws IOException {
        ByteBuffer lengthHeader = ByteBuffer.allocate(Integer.BYTES);
        long headerPosition = getSectorNumber(offset) * 4096L;
        while (lengthHeader.hasRemaining()) {
            int read = this.file.read(lengthHeader, headerPosition + lengthHeader.position());
            if (read <= 0) {
                throw new IOException("Truncated legacy RegionFile chunk length header");
            }
        }

        int storedLength = lengthHeader.getInt(0);
        if (storedLength <= 0) {
            throw new IOException("Invalid legacy RegionFile chunk length: " + storedLength);
        }

        long bytesIncludingHeader = (long) storedLength + Integer.BYTES;
        long sectors = (bytesIncludingHeader + 4095L) / 4096L;
        if (sectors <= 0 || sectors > Integer.MAX_VALUE / 4096) {
            throw new IOException("Legacy RegionFile chunk sector range is not representable: " + sectors);
        }
        return (int) sectors;
    }

    @Decorate(method = "<init>(Lnet/minecraft/world/level/chunk/storage/RegionStorageInfo;Ljava/nio/file/Path;Ljava/nio/file/Path;Lnet/minecraft/world/level/chunk/storage/RegionFileVersion;Z)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/storage/RegionFile;getNumSectors(I)I"))
    private int arclight$extendMaxLen(int i) throws Throwable {
        int len = (int) DecorationOps.callsite().invoke(i);
        return arclight$legacySectorCount(i, len, true);
    }

    @Decorate(method = "getChunkDataInputStream",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/storage/RegionFile;getNumSectors(I)I"))
    private int arclight$extendMaxLen2(int i) throws Throwable {
        int len = (int) DecorationOps.callsite().invoke(i);
        return arclight$legacySectorCount(i, len, false);
    }
}
