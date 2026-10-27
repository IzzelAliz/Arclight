package io.izzel.arclight.common.mixin.core.world.clock;

import io.izzel.arclight.common.bridge.core.world.clock.ServerClockOwnerBridge;
import io.izzel.arclight.common.bridge.core.world.clock.ClockInstanceBridge;
import io.izzel.arclight.common.bridge.core.world.level.WorldBridge;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.bukkit.Bukkit;
import org.bukkit.event.world.TimeSkipEvent;
import java.util.function.Consumer;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.tags.TagKey;
import net.minecraft.world.clock.ClockNetworkState;
import net.minecraft.world.clock.ClockState;
import net.minecraft.world.clock.PackedClockStates;
import net.minecraft.world.clock.ServerClockManager;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.level.gamerules.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** World/global clock ownership with player-specific synchronization and scoped sleep events. */
@Mixin(ServerClockManager.class)
public abstract class ServerClockManagerMixin implements ServerClockOwnerBridge {
    @Shadow public abstract void init(MinecraftServer server);
    @Shadow public abstract PackedClockStates packState();
    @Shadow public abstract void setTotalTicks(Holder<WorldClock> clock, long totalTicks);

    @Shadow private MinecraftServer server;

    @Override
    public void bridge$addTicks(Holder<WorldClock> clock, long ticks) {
        ((ServerClockManagerAccessor) this).arclight$modifyClock(clock, instance -> {
            ClockInstanceBridge state = (ClockInstanceBridge) instance;
            state.bridge$totalTicks(Math.max(state.bridge$totalTicks() + ticks, 0L));
        });
    }

    @Override
    public java.util.OptionalLong bridge$timeMarker(Holder<WorldClock> clock,
            net.minecraft.resources.ResourceKey<net.minecraft.world.clock.ClockTimeMarker> marker) {
        ClockInstanceBridge instance = (ClockInstanceBridge) java.util.Objects.requireNonNull(((ServerClockManagerAccessor) this).arclight$clocks().get(clock), "No clock initialized for definition: " + clock);
        var value = instance.bridge$timeMarkers().get(marker);
        return value == null ? java.util.OptionalLong.empty()
            : java.util.OptionalLong.of(value.resolveTimeToMoveTo(instance.bridge$totalTicks()));
    }

    /** Run the supplied native/mod mutation once. Veto exits before partialTick and update tail. */
    @WrapMethod(method = "modifyClock(Lnet/minecraft/core/Holder;Ljava/util/function/Consumer;)V", require = 1, expect = 1, allow = 1)
    private void arclight$nightMutation(Holder<WorldClock> clock, Consumer<?> action, Operation<Void> original) {
        var manager = ((ServerClockManager) (Object) this);
        ServerLevel owner = (ServerLevel) ArclightCaptures.nightLevel;
        boolean sleep = owner != null && owner.dimensionType().defaultClock().map(clock::is).orElse(false);
        ArclightCaptures.nightProposal = sleep && ArclightCaptures.nightManager == manager && ArclightCaptures.nightClock == clock ? (before, proposed) -> {
            TimeSkipEvent event = new TimeSkipEvent(((WorldBridge) owner).bridge$getWorld(),
                TimeSkipEvent.SkipReason.NIGHT_SKIP, proposed - before);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                ArclightCaptures.nightCancelled = true;
                throw new ArclightCaptures.NightSkipCancelled();
            }
            return before + event.getSkipAmount();
        } : null;
        try {
            try {
                original.call(clock, action);
            } catch (ArclightCaptures.NightSkipCancelled ignored) {
                // The sleep event rejected this clock mutation.
            }
        } finally {
            ArclightCaptures.nightProposal = null;
        }
    }

    // The original marker resolver/add clamp computes the value. No synthetic algorithm or replay.
    // Strict three-site requirement: one totalTicks PUTFIELD in each actual mutation lambda.
    @WrapOperation(method = {
        "lambda$moveToTimeMarker$0(Lnet/minecraft/resources/ResourceKey;Lorg/apache/commons/lang3/mutable/MutableBoolean;Lnet/minecraft/world/clock/ServerClockManager$ClockInstance;)V",
        "lambda$setTotalTicks$0(JLnet/minecraft/world/clock/ServerClockManager$ClockInstance;)V",
        "lambda$addTicks$0(ILnet/minecraft/world/clock/ServerClockManager$ClockInstance;)V"
    }, at = @At(value = "FIELD", target = "Lnet/minecraft/world/clock/ServerClockManager$ClockInstance;totalTicks:J", opcode = Opcodes.PUTFIELD), require = 3, expect = 3, allow = 3)
    private static void arclight$nightWrite(@Coerce Object instance, long proposed, Operation<Void> original) {
        long ticks = ArclightCaptures.nightProposal == null ? proposed
            : ArclightCaptures.nightProposal.applyAsLong(((ClockInstanceBridge) instance).bridge$totalTicks(), proposed);
        original.call(instance, ticks);
    }

    @Unique private ServerLevel arclight$owner;
    @Unique private static final TagKey<WorldClock> arclight$ignoresPause = TagKey.create(
        Registries.WORLD_CLOCK, Identifier.fromNamespaceAndPath("neoforge", "ignores/pause_command"));
    @Unique private static final TagKey<WorldClock> arclight$ignoresAdvance = TagKey.create(
        Registries.WORLD_CLOCK, Identifier.fromNamespaceAndPath("neoforge", "ignores/advance_time_rule"));
    @Unique private static final boolean arclight$neoClockTags = arclight$neoClockTagsPresent();

    @Unique
    private static boolean arclight$neoClockTagsPresent() {
        try {
            Class.forName("net.neoforged.neoforge.common.Tags$WorldClocks");
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    @Override
    public void init(ServerLevel level) {
        if (this.arclight$owner != null) {
            if (this.arclight$owner != level) {
                throw new IllegalStateException("Clock manager cannot belong to two worlds");
            }
            return; // Native init recreates all instances: never overwrite previously loaded values.
        }
        this.init(level.getServer()); // Native registry/markers + packed state load; Neo holder tags retained.
        this.arclight$owner = level;
    }

    @Override
    public ServerLevel arclight$owner() {
        return this.arclight$owner;
    }

    @Inject(method = "getGameTime", at = @At("HEAD"), cancellable = true)
    private void arclight$worldGameTime(CallbackInfoReturnable<Long> cir) {
        if (this.arclight$owner != null) {
            cir.setReturnValue(this.arclight$owner.getGameTime());
        }
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getGlobalGameRules()Lnet/minecraft/world/level/gamerules/GameRules;"))
    private GameRules arclight$worldAdvanceRule(MinecraftServer server) {
        return this.arclight$owner == null ? server.getGlobalGameRules() : this.arclight$owner.getGameRules();
    }

    // Native modifyClock still performs mutation, dirty marking and cache invalidation exactly once.
    // Send player-specific packets to the selected owner scope.
    @Redirect(method = "modifyClock", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;broadcastAll(Lnet/minecraft/network/protocol/Packet;)V"))
    private void arclight$worldBroadcast(PlayerList list, Packet<?> packet) {
        if (!(packet instanceof ClientboundSetTimePacket time)) {
            list.broadcastAll(packet);
            return;
        }
        for (ServerPlayer player : this.arclight$owner == null ? list.getPlayers() : this.arclight$owner.players()) {
            Map<Holder<WorldClock>, ClockNetworkState> updates = new HashMap<>();
            for (Holder<WorldClock> clock : time.clockUpdates().keySet()) {
                ClockState state = this.packState().clocks().get(clock);
                if (state != null) {
                    updates.put(clock, this.arclight$networkState(clock, state, player));
                }
            }
            player.connection.send(new ClientboundSetTimePacket(this.arclight$gameTime(), updates));
        }
    }

    @Redirect(method = "modifyClock", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getAllLevels()Ljava/lang/Iterable;"))
    private Iterable<ServerLevel> arclight$worldCacheScope(MinecraftServer server) {
        return this.arclight$owner == null ? server.getAllLevels() : java.util.List.of(this.arclight$owner);
    }

    @Override
    public ClientboundSetTimePacket createFullSyncPacket(ServerPlayer player) {
        if (this.arclight$owner != null && player.level() != this.arclight$owner) {
            throw new IllegalArgumentException("Clock sync player belongs to a different world");
        }
        Map<Holder<WorldClock>, ClockNetworkState> clockUpdates = new HashMap<>();
        this.packState().clocks().forEach((clock, state) ->
            clockUpdates.put(clock, this.arclight$networkState(clock, state, player)));
        return new ClientboundSetTimePacket(this.arclight$gameTime(), clockUpdates);
    }

    @Unique
    private ClockNetworkState arclight$networkState(Holder<WorldClock> clock, ClockState state, ServerPlayer player) {
        // Neo's ClockInstance.ticking(boolean) uses these two tags; applying the same
        // gates here avoids incorrectly freezing mod clocks when ADVANCE_TIME is false.
        boolean advance = this.arclight$worldAdvanceRule(this.server).get(GameRules.ADVANCE_TIME);
        boolean ticking = (!state.paused() || (arclight$neoClockTags && clock.is(arclight$ignoresPause)))
            && (advance || (arclight$neoClockTags && clock.is(arclight$ignoresAdvance)));
        boolean defaultClock = player.level().dimensionType().defaultClock()
            .map(clock::is).orElse(false);
        long ticks = defaultClock ? arclight$playerTime(player, state.totalTicks()) : state.totalTicks();
        return new ClockNetworkState(ticks, state.partialTick(), ticking && (!defaultClock || player.relativeTime) ? state.rate() : 0.0F);
    }

    @Unique
    private long arclight$gameTime() {
        return this.arclight$owner == null ? this.server.overworld().getGameTime() : this.arclight$owner.getGameTime();
    }

    @Unique
    private static long arclight$playerTime(ServerPlayer player, long ticks) {
        return ((io.izzel.arclight.common.bridge.core.server.level.ServerPlayerBridge) player).bridge$getPlayerTime(ticks);
    }
}
