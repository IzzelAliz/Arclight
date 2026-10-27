package io.izzel.arclight.common.mixin.core.server.network;

import io.izzel.arclight.api.ArclightPlatform;
import io.izzel.arclight.common.mod.mixins.annotation.OnlyInPlatform;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import io.papermc.paper.adventure.ImprovedChatDecorator;
import net.minecraft.network.chat.ChatDecorator;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.FilteredText;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.FutureChain;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.concurrent.CompletableFuture;

@OnlyInPlatform({ArclightPlatform.VANILLA, ArclightPlatform.FABRIC, ArclightPlatform.NEOFORGE})
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGameChatDecorationMixin extends ServerCommonPacketListenerImplMixin {

    @Shadow public ServerPlayer player;
    @Shadow @Final private FutureChain chatMessageChain;

    @Shadow
    private void broadcastChatMessage(PlayerChatMessage message) {
        throw new AssertionError();
    }

    @Decorate(method = "lambda$handleChat$0(Lnet/minecraft/network/protocol/game/ServerboundChatPacket;Ljava/util/Optional;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/ChatDecorator;decorate(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/network/chat/Component;)Lnet/minecraft/network/chat/Component;"), require = 1, allow = 1)
    private Component arclight$prepareChatMessage(ChatDecorator decorator, ServerPlayer sender, Component content,
                                                 @Local(ordinal = 0) PlayerChatMessage signedMessage,
                                                 @Local(ordinal = 0) CompletableFuture<FilteredText> filteredFuture) throws Throwable {
        if (this.arclight$rejectChatPreparation()) {
            return (Component) DecorationOps.cancel().invoke();
        }

        // Keep native/mod decoration on its existing thread and delegate exactly once.
        Component nativeDecorated = (Component) DecorationOps.callsite().invoke(decorator, sender, content);
        // NeoForge uses null to cancel submitted chat. Retain its filter-chain barrier,
        // but do not revive the message or fire a Paper decoration event for it.
        CompletableFuture<Component> decoratedFuture = nativeDecorated == null
            ? CompletableFuture.completedFuture(null)
            : new ImprovedChatDecorator(this.server).decorate(sender, nativeDecorated);
        CompletableFuture<PlayerChatMessage> prepared = decoratedFuture.thenCombine(filteredFuture, (decorated, filtered) ->
            decorated == null ? null : signedMessage.withUnsignedContent(decorated).filter(filtered.mask()));

        // Append now, not when either future completes: completion order must not
        // change receive order. FutureChain retains its close/error handling and
        // existing chat executor; broadcastChatMessage remains the sole spam check.
        this.chatMessageChain.append(prepared, message -> {
            if (message != null && !this.arclight$rejectChatPreparation()) {
                this.broadcastChatMessage(message);
            }
        });
        // Replace only the original append tail, after validation/signature decoding.
        return (Component) DecorationOps.cancel().invoke();
    }

    @Unique
    private boolean arclight$rejectChatPreparation() {
        return this.processedDisconnect || this.player.hasDisconnected() || !this.connection.isConnected() || this.server.isStopped();
    }
}
