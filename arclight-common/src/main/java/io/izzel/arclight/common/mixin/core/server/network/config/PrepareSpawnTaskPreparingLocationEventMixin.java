package io.izzel.arclight.common.mixin.core.server.network.config;

import io.izzel.arclight.common.bridge.core.network.ConnectionSpawnLocationEventBridge;
import io.izzel.arclight.common.bridge.core.server.level.ServerPlayerBridge;
import io.izzel.arclight.common.bridge.core.server.network.config.PrepareSpawnTaskLocationEventBridge;
import io.izzel.arclight.common.bridge.core.world.level.WorldBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.papermc.paper.connection.PaperConfigurationTask;
import io.papermc.paper.event.player.AsyncPlayerSpawnLocationEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.config.PrepareSpawnTask;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.util.CraftLocation;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@Mixin(targets = "net.minecraft.server.network.config.PrepareSpawnTask$Preparing")
public abstract class PrepareSpawnTaskPreparingLocationEventMixin {

    @Shadow(remap = false) @Final private PrepareSpawnTask this$0;
    @Shadow @Final @Mutable private ServerLevel spawnLevel;
    @Shadow @Final @Mutable private CompletableFuture<Vec3> spawnPosition;
    @Shadow @Final @Mutable private Vec2 spawnAngle;

    @Unique private Future<Location> arclight$spawnLocationEvent;
    @Unique private boolean arclight$spawnLocationResolved;

    @Decorate(method = "tick()Lnet/minecraft/server/network/config/PrepareSpawnTask$Ready;",
        require = 1, expect = 1, allow = 1,
        at = @At(value = "INVOKE", target = "Ljava/util/concurrent/CompletableFuture;isDone()Z", ordinal = 0))
    private boolean arclight$awaitSpawnLocationEvent(CompletableFuture<Vec3> position) throws Throwable {
        boolean ready = (boolean) DecorationOps.callsite().invoke(position);
        if (!ready || this.arclight$spawnLocationResolved) {
            return ready;
        }
        if (this.arclight$spawnLocationEvent == null) {
            if (AsyncPlayerSpawnLocationEvent.getHandlerList().getRegisteredListeners().length == 0) {
                this.arclight$spawnLocationResolved = true;
                return true;
            }
            var task = (PrepareSpawnTaskLocationEventBridge) this.this$0;
            var connection = task.bridge$getSpawnLocationConnection();
            boolean newPlayer = task.bridge$isNewSpawnPlayer();
            // Snapshot on the native tick thread; plugin callbacks alone run in the configuration pool.
            Location initial = CraftLocation.toBukkit(position.join(), ((WorldBridge) this.spawnLevel).bridge$getWorld(), this.spawnAngle.x, this.spawnAngle.y);
            // Paper's legacy event precedes the async event. Its existing late PlayerList hook
            // consumes this connection-local marker rather than dispatching the event twice.
            var player = task.bridge$getSpawnLocationPlayer();
            boolean legacyListeners = PlayerSpawnLocationEvent.getHandlerList().getRegisteredListeners().length != 0;
            ((ConnectionSpawnLocationEventBridge) task.bridge$getSpawnLocationNetwork()).bridge$setLegacySpawnLocationHandled(!legacyListeners || player != null);
            if (legacyListeners && player != null) {
                var legacy = new PlayerSpawnLocationEvent(((ServerPlayerBridge) player).bridge$getBukkitEntity(), initial);
                legacy.callEvent();
                Location selected = legacy.getSpawnLocation();
                if (selected.getWorld() != null) {
                    this.spawnLevel = ((CraftWorld) selected.getWorld()).getHandle();
                }
                this.spawnPosition = CompletableFuture.completedFuture(new Vec3(selected.getX(), selected.getY(), selected.getZ()));
                this.spawnAngle = new Vec2(selected.getYaw(), selected.getPitch());
                initial = CraftLocation.toBukkit(this.spawnPosition.join(), ((WorldBridge) this.spawnLevel).bridge$getWorld(), this.spawnAngle.x, this.spawnAngle.y);
            }
            Location eventLocation = initial;
            this.arclight$spawnLocationEvent = PaperConfigurationTask.CONFIGURATION_POOL.submit(() -> {
                var event = new AsyncPlayerSpawnLocationEvent(connection, eventLocation, newPlayer);
                event.callEvent();
                return event.getSpawnLocation();
            });
        }
        if (!this.arclight$spawnLocationEvent.isDone()) {
            // Preserve the native null-ready exit, without blocking the native configuration tick.
            return false;
        }
        Location location;
        try {
            location = this.arclight$spawnLocationEvent.get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while selecting player spawn location", exception);
        } catch (ExecutionException exception) {
            throw new IllegalStateException("Failed to select player spawn location", exception.getCause());
        }
        // The untouched tick now loads this world's edited chunk and carries all edits into Ready.
        this.spawnLevel = ((CraftWorld) location.getWorld()).getHandle();
        this.spawnPosition = CompletableFuture.completedFuture(new Vec3(location.getX(), location.getY(), location.getZ()));
        this.spawnAngle = new Vec2(location.getYaw(), location.getPitch());
        this.arclight$spawnLocationResolved = true;
        this.arclight$spawnLocationEvent = null;
        return true;
    }

    @Decorate(method = "cancel()V", inject = true, at = @At("HEAD"), require = 1, expect = 1, allow = 1)
    private void arclight$cancelSpawnLocationEvent() {
        if (this.arclight$spawnLocationEvent != null) {
            this.arclight$spawnLocationEvent.cancel(true);
            this.arclight$spawnLocationEvent = null;
        }
    }
}
