package io.izzel.arclight.common.mixin.core.world.chunk.storage;

import io.izzel.arclight.common.bridge.world.chunk.storage.RegionFileCacheBridge;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.storage.RegionFile;
import net.minecraft.world.chunk.storage.RegionFileCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.io.File;
import java.io.IOException;

@Mixin(RegionFileCache.class)
public abstract class RegionFileCacheMixin implements RegionFileCacheBridge {

    // @formatter:off
    @Shadow protected abstract RegionFile loadFile(ChunkPos pos) throws IOException;
    // @formatter:on

    private RegionFile loadFile(ChunkPos pos, boolean existsOnly) throws IOException {
        this.arclight$existOnly = existsOnly;
        return loadFile(pos);
    }

    private transient boolean arclight$existOnly;

    @Inject(method = "loadFile", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD,
        at = @At(value = "NEW", target = "net/minecraft/world/chunk/storage/RegionFile"))
    private void arclight$retIfSearch(ChunkPos pos, CallbackInfoReturnable<RegionFile> cir, long l, File file) {
        if (arclight$existOnly && !file.exists()) cir.setReturnValue(null);
    }

    @Inject(method = "readChunk", at = @At("HEAD"))
    private void arclight$read(ChunkPos pos, CallbackInfoReturnable<CompoundNBT> cir) {
        this.arclight$existOnly = true;
    }

    @Inject(method = "readChunk", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/storage/RegionFile;func_222666_a(Lnet/minecraft/util/math/ChunkPos;)Ljava/io/DataInputStream;"))
    private void arclight$retIfNotFound(ChunkPos pos, CallbackInfoReturnable<CompoundNBT> cir, RegionFile rf) {
        if (rf == null) {
            cir.setReturnValue(null);
        }
    }

    @Inject(method = "writeChunk", at = @At("HEAD"))
    private void arclight$write(ChunkPos pos, CompoundNBT compound, CallbackInfo ci) {
        this.arclight$existOnly = false;
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
