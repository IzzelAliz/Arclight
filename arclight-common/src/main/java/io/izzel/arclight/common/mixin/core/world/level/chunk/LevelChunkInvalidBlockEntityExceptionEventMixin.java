package io.izzel.arclight.common.mixin.core.world.level.chunk;

import com.destroystokyo.paper.exception.ServerInternalException;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.world.level.chunk.LevelChunk;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LevelChunk.class)
public abstract class LevelChunkInvalidBlockEntityExceptionEventMixin {

    // Only the first warning is Paper's original !hasBlockEntity producer.
    // The later type-invalid and state-mismatch warnings remain independent.
    @Decorate(method = "setBlockEntity(Lnet/minecraft/world/level/block/entity/BlockEntity;)V",
        require = 1, allow = 1,
        at = @At(value = "INVOKE", ordinal = 0,
            target = "Lorg/slf4j/Logger;warn(Ljava/lang/String;[Ljava/lang/Object;)V", remap = false))
    private void arclight$invalidBlockEntityException(Logger logger, String message, Object[] arguments) throws Throwable {
        DecorationOps.callsite().invoke(logger, message, arguments);
        try {
            ServerInternalException.reportInternalException(new ServerInternalException(
                "Trying to set block entity %s at position %s, but state %s does not allow it".formatted(arguments)));
        } catch (Throwable ignored) {
            // Leave the native warning-only rejection intact if reporting fails.
        }
    }
}
