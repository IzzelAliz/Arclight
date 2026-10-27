package io.izzel.arclight.common.mixin.core.world.level.chunk.storage;

import com.destroystokyo.paper.exception.ServerInternalException;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.world.level.chunk.storage.RegionFile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;

@Mixin(RegionFile.class)
public abstract class RegionFileExceptionEventMixin {

    @Decorate(method = "doesChunkExist(Lnet/minecraft/world/level/ChunkPos;)Z", require = 1, allow = 1,
        at = @At(value = "INVOKE", target = "Ljava/nio/channels/FileChannel;read(Ljava/nio/ByteBuffer;J)I", remap = false))
    private int arclight$regionReadException(FileChannel file, ByteBuffer buffer, long position) throws Throwable {
        try {
            return (int) DecorationOps.callsite().invoke(file, buffer, position);
        } catch (IOException cause) {
            try {
                ServerInternalException.reportInternalException(cause);
            } catch (Throwable ignored) {
                // Preserve the existing IOException-to-false recovery below this call.
            }
            throw cause;
        }
    }

    @Inject(method = "writeToExternalFile(Ljava/nio/file/Path;Ljava/nio/ByteBuffer;)Lnet/minecraft/world/level/chunk/storage/RegionFile$CommitOp;",
        at = @At(value = "INVOKE", shift = At.Shift.AFTER,
            target = "Ljava/nio/file/Files;createTempFile(Ljava/nio/file/Path;Ljava/lang/String;Ljava/lang/String;[Ljava/nio/file/attribute/FileAttribute;)Ljava/nio/file/Path;", remap = false),
        require = 1, allow = 1)
    private void arclight$externalWriteStart(Path path, ByteBuffer data, CallbackInfoReturnable<Object> cir,
            @Share("arclight$externalWriteStarted") LocalBooleanRef started) {
        // Paper's catch includes resource-initializer argument evaluation, but
        // not createTempFile itself. Mark before the OpenOption array is built.
        started.set(true);
    }

    @Inject(method = "writeToExternalFile(Ljava/nio/file/Path;Ljava/nio/ByteBuffer;)Lnet/minecraft/world/level/chunk/storage/RegionFile$CommitOp;",
        at = @At(value = "INVOKE", target = "Ljava/nio/channels/FileChannel;close()V", ordinal = 0, shift = At.Shift.AFTER, remap = false),
        require = 1, allow = 1)
    private void arclight$externalWriteFinished(Path path, ByteBuffer data, CallbackInfoReturnable<Object> cir,
            @Share("arclight$externalWriteStarted") LocalBooleanRef started) {
        // FileChannel.open returns a non-null channel. This normal close is
        // before CommitOp construction; the exceptional close is a later site.
        started.set(false);
    }

    @WrapMethod(method = "writeToExternalFile(Ljava/nio/file/Path;Ljava/nio/ByteBuffer;)Lnet/minecraft/world/level/chunk/storage/RegionFile$CommitOp;")
    @Coerce
    private Object arclight$externalWriteException(Path path, ByteBuffer data, Operation<Object> original,
            @Share("arclight$externalWriteStarted") LocalBooleanRef started) throws Throwable {
        try {
            return original.call(path, data);
        } catch (Throwable cause) {
            if (started.get()) {
                try {
                    ServerInternalException.reportInternalException(cause);
                } catch (Throwable ignored) {
                    // The original write failure (including suppressed close failures) wins.
                }
            }
            throw cause;
        }
    }
}
