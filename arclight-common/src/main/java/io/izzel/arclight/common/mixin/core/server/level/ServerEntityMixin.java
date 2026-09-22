package io.izzel.arclight.common.mixin.core.server.level;

import io.izzel.arclight.common.bridge.core.server.level.ServerEntityBridge;
import io.izzel.arclight.common.bridge.core.server.level.ServerPlayerBridge;
import io.izzel.arclight.common.mod.ArclightConstants;
import io.izzel.arclight.common.mod.mixins.annotation.CreateConstructor;
import io.izzel.arclight.common.mod.mixins.annotation.ShadowConstructor;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.util.Vector;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Mixin(ServerEntity.class)
public abstract class ServerEntityMixin implements ServerEntityBridge {

    // @formatter:off
    @Shadow @Final private Entity entity;
    @Shadow @Final private ServerLevel level;
    @Shadow @Final private ServerEntity.Synchronizer synchronizer;
    @Shadow private int tickCount;
    @Shadow protected abstract void sendDirtyEntityData();
    // @formatter:on

    @Unique private Set<ServerPlayerConnection> trackedPlayers;
    @Unique private int lastTick;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void arclight$init(ServerLevel level, Entity entity, int updateInterval, boolean trackDelta, ServerEntity.Synchronizer synchronizer, CallbackInfo ci) {
        this.trackedPlayers = new HashSet<>();
        this.lastTick = ArclightConstants.currentTick - 1;
    }

    @ShadowConstructor
    public void arclight$constructor(ServerLevel level, Entity entity, int updateInterval, boolean trackDelta, ServerEntity.Synchronizer synchronizer) {
        throw new NullPointerException();
    }

    @CreateConstructor
    public void arclight$constructor(ServerLevel level, Entity entity, int updateInterval, boolean trackDelta, ServerEntity.Synchronizer synchronizer, Set<ServerPlayerConnection> trackedPlayers) {
        arclight$constructor(level, entity, updateInterval, trackDelta, synchronizer);
        this.trackedPlayers = trackedPlayers;
    }

    @Override
    public void bridge$setTrackedPlayers(Set<ServerPlayerConnection> trackedPlayers) {
        this.trackedPlayers = trackedPlayers;
    }

    /**
     * Preserve the realtime optimization's elapsed-tick accounting without replacing
     * 1.21.11's sendChanges body (notably precise positioning and new minecart sync).
     */
    @Inject(method = "sendChanges", at = @At(value = "FIELD", shift = At.Shift.AFTER, opcode = Opcodes.PUTFIELD, target = "Lnet/minecraft/server/level/ServerEntity;tickCount:I"))
    private void arclight$elapsedTickOptimization(CallbackInfo ci) {
        int elapsedTicks = ArclightConstants.currentTick - this.lastTick;
        if (elapsedTicks < 0) {
            elapsedTicks = 0;
        }
        this.lastTick = ArclightConstants.currentTick;
        // Vanilla just incremented once. Adjust after that increment so its current
        // packet/map interval decisions used the same pre-increment tick as before.
        this.tickCount += elapsedTicks - 1;
    }

    /**
     * Vanilla updates item-frame maps for every level player. Bukkit must only update
     * connections currently tracking this frame, while retaining the vanilla map and
     * dirty-data flow around this call.
     */
    @Decorate(method = "sendChanges", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;players()Ljava/util/List;"))
    private List<ServerPlayer> arclight$trackedPlayers(ServerLevel level) throws Throwable {
        List<ServerPlayer> players = (List<ServerPlayer>) DecorationOps.callsite().invoke(level);
        if (this.trackedPlayers == null) {
            return players;
        }
        List<ServerPlayer> tracked = new ArrayList<>(this.trackedPlayers.size());
        for (ServerPlayerConnection connection : this.trackedPlayers) {
            tracked.add(connection.getPlayer());
        }
        return tracked;
    }

    /**
     * Spigot's passenger packet path is filtered-and-self: the predicate still limits
     * tracking viewers, but a player that is the tracked entity must receive its own
     * packet even when that predicate rejects it. Vanilla exposes only the filtered
     * Synchronizer call, so preserve that exact callsite and append the self delivery.
     */
    @Decorate(method = "sendChanges", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerEntity$Synchronizer;sendToTrackingPlayersFiltered(Lnet/minecraft/network/protocol/Packet;Ljava/util/function/Predicate;)V"))
    private void arclight$passengerPacket(ServerEntity.Synchronizer synchronizer, Packet<? super ClientGamePacketListener> packet, Predicate<ServerPlayer> predicate) throws Throwable {
        DecorationOps.callsite().invoke(synchronizer, packet, predicate);
        if (this.entity instanceof ServerPlayer player) {
            player.connection.send(packet);
        }
    }

    /**
     * Keep Bukkit's PlayerVelocityEvent on the final motion packet. This callsite is
     * the 1.21.11 replacement for broadcastAndSend: it reaches tracking players and
     * the subject, and the packet is rebuilt after a plugin changes velocity.
     */
    @Decorate(method = "sendChanges", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerEntity$Synchronizer;sendToTrackingPlayersAndSelf(Lnet/minecraft/network/protocol/Packet;)V"))
    private void arclight$velocityEvent(ServerEntity.Synchronizer synchronizer, Packet<? super ClientGamePacketListener> packet) throws Throwable {
        if (this.entity instanceof ServerPlayer player) {
            Player bukkitPlayer = ((ServerPlayerBridge) player).bridge$getBukkitEntity();
            Vector velocity = bukkitPlayer.getVelocity();
            PlayerVelocityEvent event = new PlayerVelocityEvent(bukkitPlayer, velocity.clone());
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                DecorationOps.cancel().invoke();
                return;
            }
            if (!velocity.equals(event.getVelocity())) {
                bukkitPlayer.setVelocity(event.getVelocity());
                packet = new ClientboundSetEntityMotionPacket(this.entity);
            }
        }
        DecorationOps.callsite().invoke(synchronizer, packet);
    }

    /**
     * The attributes packet copies the collection in its constructor. Decorate the
     * collection's emptiness check, which runs after getAttributesToSync() but before
     * that constructor, so scaled max health is part of the packet snapshot.
     */
    @Decorate(method = "sendDirtyEntityData", at = @At(value = "INVOKE", target = "Ljava/util/Set;isEmpty()Z"))
    private boolean arclight$sendScaledHealth(Set<AttributeInstance> attributes) throws Throwable {
        if (this.entity instanceof ServerPlayerBridge player) {
            player.bridge$getBukkitEntity().injectScaledMaxHealth(attributes, false);
        }
        return (boolean) DecorationOps.callsite().invoke(attributes);
    }

    @Inject(method = "addPairing", cancellable = true, require = 0, at = @At("HEAD"))
    private void arclight$returnIfRemoved(CallbackInfo ci) {
        if (this.entity.isRemoved()) {
            ci.cancel();
        }
    }

    @Decorate(method = "sendPairingData", at = @At(value = "INVOKE", target = "Ljava/util/Collection;isEmpty()Z"))
    private boolean arclight$injectScaledHealth(Collection<AttributeInstance> attributes, ServerPlayer player, Consumer<Packet<? super ClientGamePacketListener>> broadcast) throws Throwable {
        if (this.entity.getId() == player.getId() && this.entity instanceof ServerPlayerBridge bridge) {
            bridge.bridge$getBukkitEntity().injectScaledMaxHealth(attributes, false);
        }
        return (boolean) DecorationOps.callsite().invoke(attributes);
    }
}
