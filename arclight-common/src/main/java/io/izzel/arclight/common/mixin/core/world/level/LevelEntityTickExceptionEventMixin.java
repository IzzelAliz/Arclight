package io.izzel.arclight.common.mixin.core.world.level;

import com.destroystokyo.paper.event.server.ServerExceptionEvent;
import com.destroystokyo.paper.exception.ServerInternalException;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.Local;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.bukkit.Bukkit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Consumer;

@Mixin(Level.class)
public abstract class LevelEntityTickExceptionEventMixin {

    @Decorate(method = "guardEntityTick(Ljava/util/function/Consumer;Lnet/minecraft/world/entity/Entity;)V",
        inject = true, require = 1, allow = 1,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/CrashReport;forThrowable(Ljava/lang/Throwable;Ljava/lang/String;)Lnet/minecraft/CrashReport;"))
    private <T extends Entity> void arclight$entityTickException(Consumer<T> tick, T entity,
            @Local(ordinal = -1) Throwable cause) {
        try {
            Level level = (Level) (Object) this;
            if (!level.isClientSide()) {
                String message = String.format("Entity threw exception at %s:%s,%s,%s",
                    entity.level().dimension().identifier(), entity.getX(), entity.getY(), entity.getZ());
                Bukkit.getPluginManager().callEvent(new ServerExceptionEvent(new ServerInternalException(message, cause)));
            }
        } catch (Throwable reportingFailure) {
            // Notification failure must not replace the caught tick failure or
            // bypass vanilla's crash / NeoForge's configured removal branch.
            try {
                Bukkit.getLogger().log(java.util.logging.Level.WARNING, "Exception posting ServerExceptionEvent", reportingFailure);
            } catch (Throwable ignored) {
                // Even a broken diagnostic logger must leave that native branch intact.
            }
        }
    }
}
