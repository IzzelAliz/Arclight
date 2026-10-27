package io.izzel.arclight.common.mixin.core.server.network;

import com.destroystokyo.paper.event.brigadier.AsyncPlayerSendSuggestionsEvent;
import com.destroystokyo.paper.event.server.AsyncTabCompleteEvent;
import com.google.common.primitives.Ints;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.izzel.arclight.common.bridge.core.server.MinecraftServerBridge;
import io.izzel.arclight.common.bridge.core.server.network.ServerCommonPacketListenerImplBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import io.papermc.paper.adventure.PaperAdventure;
import net.minecraft.network.protocol.game.ClientboundCommandSuggestionsPacket;
import net.minecraft.network.protocol.game.ServerboundCommandSuggestionPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spigotmc.SpigotConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerSuggestionsMixin {

    @Unique
    private static final ExecutorService arclight$tabCompleteExecutor = Executors.newFixedThreadPool(4,
        new ThreadFactoryBuilder().setDaemon(true).setNameFormat("Arclight Async Tab Complete - %d").build());

    @Unique
    private final net.minecraft.util.TickThrottler arclight$tabSpamThrottler = arclight$createTabSpamThrottler();

    @Unique
    private static net.minecraft.util.TickThrottler arclight$createTabSpamThrottler() {
        var configuration = io.papermc.paper.configuration.GlobalConfiguration.get();
        return new net.minecraft.util.TickThrottler(
            configuration.spamLimiter.tabSpamIncrement,
            configuration.spamLimiter.tabSpamLimit);
    }

    @Decorate(method = "tick()V", inject = true,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/util/TickThrottler;tick()V", ordinal = 1, shift = At.Shift.AFTER),
        require = 1, allow = 1)
    private void arclight$decayTabSpam() {
        synchronized (this.arclight$tabSpamThrottler) {
            this.arclight$tabSpamThrottler.tick();
        }
    }

    @WrapMethod(method = "handleCustomCommandSuggestions(Lnet/minecraft/network/protocol/game/ServerboundCommandSuggestionPacket;)V",
        require = 1, expect = 1, allow = 1)
    private void arclight$asyncTabComplete(ServerboundCommandSuggestionPacket packet, Operation<Void> original) {
        var listener = (ServerCommonPacketListenerImplBridge) this;
        MinecraftServer server = listener.bridge$getCraftServer().getServer();
        var player = listener.bridge$getPlayer();
        boolean underThreshold;
        synchronized (this.arclight$tabSpamThrottler) {
            this.arclight$tabSpamThrottler.increment();
            underThreshold = this.arclight$tabSpamThrottler.isUnderThreshold();
        }
        if (!underThreshold && !server.getPlayerList().isOp(player.nameAndId()) && !server.isSingleplayerOwner(player.nameAndId())) {
            arclight$disconnectTabSpam(server, listener);
            return;
        }
        if (SpigotConfig.tabComplete < 0) {
            return;
        }
        int space = packet.getCommand().indexOf(' ');
        if (packet.getCommand().length() > 64 && (space == -1 || space >= 64)) {
            arclight$disconnectTabSpam(server, listener);
            return;
        }
        // Only the async event runs on this pool. Unhandled requests resume the actual
        // native/mod body on main, including its thread check, parser and permissions.
        arclight$tabCompleteExecutor.execute(() -> {
            if (listener.bridge$processedDisconnect()) {
                return;
            }
            var event = new AsyncTabCompleteEvent(listener.bridge$getCraftPlayer(), packet.getCommand(), true, null);
            event.callEvent();
            if (event.isCancelled()) {
                return;
            }
            if (!event.isHandled()) {
                ((MinecraftServerBridge) server).bridge$queuedProcess(() -> {
                    if (!listener.bridge$processedDisconnect()) {
                        original.call(packet);
                    }
                });
                return;
            }
            // Handled-but-empty suppresses the native process without sending a packet,
            // matching Paper's AsyncTabCompleteEvent producer.
            if (event.completions().isEmpty()) {
                return;
            }
            String input = packet.getCommand();
            var builder = new SuggestionsBuilder(input, input.length()).createOffset(input.lastIndexOf(' ') + 1);
            for (var completion : event.completions()) {
                Integer integer = Ints.tryParse(completion.suggestion());
                if (integer != null) {
                    builder.suggest(integer, PaperAdventure.asVanilla(completion.tooltip()));
                } else {
                    builder.suggest(completion.suggestion(), PaperAdventure.asVanilla(completion.tooltip()));
                }
            }
            // build() is immediate; never block the pool/main thread waiting on a future.
            Suggestions suggestions = arclight$sendSuggestionsEvent(packet, builder.build());
            if (suggestions != null && !listener.bridge$processedDisconnect()) {
                ((ServerGamePacketListenerImpl) (Object) this).send(new ClientboundCommandSuggestionsPacket(packet.getId(), arclight$limitSuggestions(suggestions)));
            }
        });
    }

    @Decorate(method = "handleCustomCommandSuggestions(Lnet/minecraft/network/protocol/game/ServerboundCommandSuggestionPacket;)V",
        at = @At(value = "INVOKE", target = "Ljava/util/concurrent/CompletableFuture;thenAccept(Ljava/util/function/Consumer;)Ljava/util/concurrent/CompletableFuture;", remap = false),
        require = 1, allow = 1)
    private CompletableFuture<Void> arclight$nativeSuggestionsEvent(CompletableFuture<Suggestions> future, Consumer<Suggestions> consumer,
                                                                  @Local(ordinal = 0) ServerboundCommandSuggestionPacket packet) throws Throwable {
        // Retain the native future and its completion thread. The original consumer
        // still caps and sends once, but only receives the listener's edited result.
        Consumer<Suggestions> eventConsumer = results -> {
            if (!SpigotConfig.sendNamespaced && results.getRange().getStart() <= 1) {
                var visible = new ArrayList<>(results.getList());
                visible.removeIf(suggestion -> suggestion.getText().contains(":"));
                results = new Suggestions(results.getRange(), visible);
            }
            Suggestions suggestions = arclight$sendSuggestionsEvent(packet, results);
            if (suggestions != null && !((ServerCommonPacketListenerImplBridge) this).bridge$processedDisconnect()) {
                consumer.accept(suggestions);
            }
        };
        return (CompletableFuture<Void>) DecorationOps.callsite().invoke(future, eventConsumer);
    }

    @Unique
    private Suggestions arclight$sendSuggestionsEvent(ServerboundCommandSuggestionPacket packet, Suggestions suggestions) {
        var listener = (ServerCommonPacketListenerImplBridge) this;
        var event = new AsyncPlayerSendSuggestionsEvent(listener.bridge$getCraftPlayer(), suggestions, packet.getCommand());
        event.setCancelled(suggestions.isEmpty());
        return event.callEvent() ? event.getSuggestions() : null;
    }

    @Unique
    private static void arclight$disconnectTabSpam(MinecraftServer server, ServerCommonPacketListenerImplBridge listener) {
        // The existing disconnect bridge is synchronous and may wait off-main;
        // enqueue it instead so this packet's async path never blocks a network thread.
        ((MinecraftServerBridge) server).bridge$queuedProcess(() -> listener.bridge$disconnect(
            net.minecraft.network.chat.Component.translatable("disconnect.spam"), org.bukkit.event.player.PlayerKickEvent.Cause.SPAM));
    }

    @Unique
    private static Suggestions arclight$limitSuggestions(Suggestions suggestions) {
        return suggestions.getList().size() <= 1000 ? suggestions
            : new Suggestions(suggestions.getRange(), suggestions.getList().subList(0, 1000));
    }
}
