package io.izzel.arclight.common.mixin.core.world.level.border;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.izzel.arclight.common.bridge.core.world.level.border.WorldBorderBridge;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.border.BorderChangeListener;
import net.minecraft.world.level.border.WorldBorder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(WorldBorder.class)
public abstract class WorldBorderMixin implements WorldBorderBridge {

    @Shadow @Final private List<BorderChangeListener> listeners;
    public Level world;

    @Shadow public abstract void applyInitialSettings(long gameTime);

    // owner, force, claimed: a request is consumed by exactly one native entry.
    @Unique private static final ThreadLocal<Object[]> arclight$initialSettings = new ThreadLocal<>();

    public void applyInitialSettings(long gameTime, boolean force) {
        Object[] previous = arclight$initialSettings.get();
        arclight$initialSettings.set(new Object[]{this, force, false});
        try {
            this.applyInitialSettings(gameTime);
        } finally {
            arclight$restoreInitialSettings(previous);
        }
    }

    @WrapMethod(method = "applyInitialSettings(J)V")
    private void arclight$initialSettingsEntry(long gameTime, Operation<Void> original) {
        Object[] previous = arclight$initialSettings.get();
        boolean force = previous != null && previous[0] == this
            && Boolean.FALSE.equals(previous[2]) && Boolean.TRUE.equals(previous[1]);
        if (previous != null && previous[0] == this) {
            previous[2] = true;
        }
        // Direct native reentry must not inherit an outer forced entry.
        arclight$initialSettings.set(new Object[]{this, force, true});
        try {
            original.call(gameTime);
        } finally {
            arclight$restoreInitialSettings(previous);
        }
    }

    @Unique
    private static void arclight$restoreInitialSettings(Object[] previous) {
        if (previous == null) {
            arclight$initialSettings.remove();
        } else {
            arclight$initialSettings.set(previous);
        }
    }

    @ModifyExpressionValue(method = "applyInitialSettings(J)V", at = @At(value = "FIELD",
        target = "Lnet/minecraft/world/level/border/WorldBorder;initialized:Z", opcode = 180, ordinal = 0))
    private boolean arclight$initialSettingsGuard(boolean initialized) {
        Object[] entry = arclight$initialSettings.get();
        return initialized && !(entry != null && entry[0] == this && Boolean.TRUE.equals(entry[1]));
    }

    @Override
    public Level bridge$getWorld() {
        return this.world;
    }

    @Override
    public void bridge$setWorld(Level world) {
        this.world = world;
    }

    @Inject(method = "addListener", cancellable = true, at = @At("HEAD"))
    private void arclight$removeDuplicateListener(BorderChangeListener listener, CallbackInfo ci) {
        if (listeners.contains(listener)) {
            ci.cancel();
        }
    }
}
