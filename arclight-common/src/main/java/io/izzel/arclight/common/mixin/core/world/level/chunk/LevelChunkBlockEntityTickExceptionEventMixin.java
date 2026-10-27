package io.izzel.arclight.common.mixin.core.world.level.chunk;

import com.destroystokyo.paper.event.server.ServerExceptionEvent;
import com.destroystokyo.paper.exception.ServerInternalException;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.bukkit.Bukkit;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.world.level.chunk.LevelChunk$BoundTickingBlockEntity")
public abstract class LevelChunkBlockEntityTickExceptionEventMixin {

    @Shadow(remap = false) @Final private LevelChunk this$0;
    @Shadow public abstract BlockPos getPos();

    @Decorate(method = "tick()V", inject = true, require = 1, allow = 1,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/CrashReport;forThrowable(Ljava/lang/Throwable;Ljava/lang/String;)Lnet/minecraft/CrashReport;"))
    private void arclight$blockEntityTickException(@Local(ordinal = -1) Throwable cause) {
        try {
            Level level = this.this$0.getLevel();
            if (!level.isClientSide()) {
                BlockPos pos = this.getPos();
                String message = String.format("BlockEntity threw exception at %s:%s,%s,%s",
                    level.dimension().identifier(), pos.getX(), pos.getY(), pos.getZ());
                Bukkit.getPluginManager().callEvent(new ServerExceptionEvent(new ServerInternalException(message, cause)));
            }
        } catch (Throwable reportingFailure) {
            // Only the notification is guarded; native crash reporting, mod
            // removal configuration and TimeTracker's finally remain untouched.
            try {
                Bukkit.getLogger().log(java.util.logging.Level.WARNING, "Exception posting ServerExceptionEvent", reportingFailure);
            } catch (Throwable ignored) {
                // Do not allow diagnostic failure to mask the original tick failure.
            }
        }
    }
}
