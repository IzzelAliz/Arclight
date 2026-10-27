package io.izzel.arclight.common.mixin.core.world.level.border;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.izzel.arclight.common.bridge.core.world.level.WorldBridge;
import io.izzel.arclight.common.bridge.core.world.level.border.WorldBorderBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.papermc.paper.event.world.border.WorldBorderBoundsChangeEvent;
import io.papermc.paper.event.world.border.WorldBorderCenterChangeEvent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.border.BorderChangeListener;
import net.minecraft.world.level.border.WorldBorder;
import org.bukkit.Location;
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
    @Shadow public abstract double getCenterX();
    @Shadow public abstract double getCenterZ();
    @Shadow public abstract double getSize();
    @Shadow public abstract void lerpSizeBetween(double from, double to, long ticks, long gameTime);

    @Decorate(method = "setCenter(DD)V", inject = true, at = @At("HEAD"), require = 1, allow = 1)
    private void arclight$centerChange(double x, double z) throws Throwable {
        if (this.world != null) {
            var bukkitWorld = ((WorldBridge) this.world).bridge$getWorld();
            var event = new WorldBorderCenterChangeEvent(bukkitWorld, bukkitWorld.getWorldBorder(),
                new Location(bukkitWorld, this.getCenterX(), 0, this.getCenterZ()), new Location(bukkitWorld, x, 0, z));
            if (!event.callEvent()) {
                DecorationOps.cancel().invoke();
                return;
            }
            x = event.getNewCenter().getX();
            z = event.getNewCenter().getZ();
            DecorationOps.blackhole().invoke(x, z);
        }
    }

    @Decorate(method = "setSize(D)V", inject = true, at = @At("HEAD"), require = 1, allow = 1)
    private void arclight$boundsChange(double size) throws Throwable {
        if (this.world != null) {
            var bukkitWorld = ((WorldBridge) this.world).bridge$getWorld();
            var event = new WorldBorderBoundsChangeEvent(bukkitWorld, bukkitWorld.getWorldBorder(),
                WorldBorderBoundsChangeEvent.Type.INSTANT_MOVE, this.getSize(), size, 0);
            if (!event.callEvent()) {
                DecorationOps.cancel().invoke();
                return;
            }
            if (event.getType() == WorldBorderBoundsChangeEvent.Type.STARTED_MOVE && event.getDurationTicks() > 0) {
                // Paper re-enters the native moving setter, including its cancellable/editable event.
                this.lerpSizeBetween(event.getOldSize(), event.getNewSize(), event.getDurationTicks(), this.world.getGameTime());
                DecorationOps.cancel().invoke();
                return;
            }
            size = event.getNewSize();
            DecorationOps.blackhole().invoke(size);
        }
    }

    @Decorate(method = "lerpSizeBetween(DDJJ)V", inject = true, at = @At("HEAD"), require = 1, allow = 1)
    private void arclight$movingBoundsChange(double from, double to, long ticks, long gameTime) throws Throwable {
        if (this.world != null) {
            var bukkitWorld = ((WorldBridge) this.world).bridge$getWorld();
            var type = from == to ? WorldBorderBoundsChangeEvent.Type.INSTANT_MOVE : WorldBorderBoundsChangeEvent.Type.STARTED_MOVE;
            var event = new WorldBorderBoundsChangeEvent(bukkitWorld, bukkitWorld.getWorldBorder(), type, from, to, ticks);
            if (!event.callEvent()) {
                DecorationOps.cancel().invoke();
                return;
            }
            to = event.getNewSize();
            // Both the native setter and the non-deprecated event accessors use ticks, not milliseconds.
            ticks = event.getDurationTicks();
            DecorationOps.blackhole().invoke(to, ticks);
        }
    }

    // owner, force, claimed: a request is consumed by exactly one native entry.
    @Unique private static Object[] arclight$initialSettings;

    public void applyInitialSettings(long gameTime, boolean force) {
        Object[] previous = arclight$initialSettings;
        arclight$initialSettings = new Object[]{this, force, false};
        try {
            this.applyInitialSettings(gameTime);
        } finally {
            arclight$restoreInitialSettings(previous);
        }
    }

    @WrapMethod(method = "applyInitialSettings(J)V")
    private void arclight$initialSettingsEntry(long gameTime, Operation<Void> original) {
        Object[] previous = arclight$initialSettings;
        boolean force = previous != null && previous[0] == this
            && Boolean.FALSE.equals(previous[2]) && Boolean.TRUE.equals(previous[1]);
        if (previous != null && previous[0] == this) {
            previous[2] = true;
        }
        // Direct native reentry must not inherit an outer forced entry.
        arclight$initialSettings = new Object[]{this, force, true};
        try {
            original.call(gameTime);
        } finally {
            arclight$restoreInitialSettings(previous);
        }
    }

    @Unique
    private static void arclight$restoreInitialSettings(Object[] previous) {
        arclight$initialSettings = previous;
    }

    @ModifyExpressionValue(method = "applyInitialSettings(J)V", at = @At(value = "FIELD",
        target = "Lnet/minecraft/world/level/border/WorldBorder;initialized:Z", opcode = 180, ordinal = 0))
    private boolean arclight$initialSettingsGuard(boolean initialized) {
        Object[] entry = arclight$initialSettings;
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
