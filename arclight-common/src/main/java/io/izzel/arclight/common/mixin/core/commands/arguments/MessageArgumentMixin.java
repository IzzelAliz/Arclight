package io.izzel.arclight.common.mixin.core.commands.arguments;

import com.mojang.datafixers.util.Pair;
import io.izzel.arclight.common.mod.util.CommandMessageTask;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import io.papermc.paper.adventure.ImprovedChatDecorator;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.network.chat.ChatDecorator;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.FilteredText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Mixin(MessageArgument.class)
public class MessageArgumentMixin {

    @Decorate(method = "resolveSignedMessage(Ljava/util/function/Consumer;Lnet/minecraft/commands/CommandSourceStack;Lnet/minecraft/network/chat/PlayerChatMessage;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/ChatDecorator;decorate(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/network/chat/Component;)Lnet/minecraft/network/chat/Component;"), require = 1, allow = 1)
    private static Component arclight$decorateSignedMessage(ChatDecorator decorator, ServerPlayer player, Component content,
                                                           @Local(ordinal = 0) Consumer<PlayerChatMessage> task,
                                                           @Local(ordinal = 0) CommandSourceStack sender,
                                                           @Local(ordinal = 0) PlayerChatMessage signedArgument,
                                                           @Local(ordinal = 0) CompletableFuture<FilteredText> filteredFuture) throws Throwable {
        // Keep the native/mod decorator on its original thread, before Paper's async event.
        Component nativeDecorated = (Component) DecorationOps.callsite().invoke(decorator, player, content);
        if (nativeDecorated == null) {
            if (task instanceof CompletableFuture<?> resolution) {
                resolution.cancel(false);
            }
            // A cancelling decorator must not reach Paper; retain the running filter's chain barrier.
            sender.getChatMessageChainer().append(filteredFuture, filtered -> {});
            return (Component) DecorationOps.cancel().invoke();
        }
        CompletableFuture<Component> decoratedFuture = new ImprovedChatDecorator(sender.getServer()).decorate(player, sender, nativeDecorated);
        // Observe each input: thenCombine can wait forever for the other one after a failure.
        CommandMessageTask.observeFailure(task, filteredFuture);
        CommandMessageTask.observeFailure(task, decoratedFuture);
        CompletableFuture<Pair<Component, FilteredText>> preparation = decoratedFuture.thenCombine(filteredFuture, Pair::of);
        CommandMessageTask.append(sender.getChatMessageChainer(), preparation, result -> {
            PlayerChatMessage filteredMessage = signedArgument.withUnsignedContent(result.getFirst()).filter(result.getSecond().mask());
            task.accept(filteredMessage);
        }, task);
        // The native filter future is already running; replace only the synchronous tail.
        return (Component) DecorationOps.cancel().invoke();
    }

    @Decorate(method = "resolveDisguisedMessage(Ljava/util/function/Consumer;Lnet/minecraft/commands/CommandSourceStack;Lnet/minecraft/network/chat/PlayerChatMessage;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/ChatDecorator;decorate(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/network/chat/Component;)Lnet/minecraft/network/chat/Component;"), require = 1, allow = 1)
    private static Component arclight$decorateDisguisedMessage(ChatDecorator decorator, ServerPlayer player, Component content,
                                                              @Local(ordinal = 0) Consumer<PlayerChatMessage> task,
                                                              @Local(ordinal = 0) CommandSourceStack sender,
                                                              @Local(ordinal = 0) PlayerChatMessage argument) throws Throwable {
        Component nativeDecorated = (Component) DecorationOps.callsite().invoke(decorator, player, content);
        if (nativeDecorated == null) {
            if (task instanceof CompletableFuture<?> resolution) {
                resolution.cancel(false);
            }
            // Disguised messages have no filter barrier; cancellation must not run the task.
            return (Component) DecorationOps.cancel().invoke();
        }
        CompletableFuture<Component> decoratedFuture = new ImprovedChatDecorator(sender.getServer()).decorate(player, sender, nativeDecorated);
        CommandMessageTask.append(sender.getChatMessageChainer(), decoratedFuture,
            result -> task.accept(argument.withUnsignedContent(result)), task);
        return (Component) DecorationOps.cancel().invoke();
    }
}
