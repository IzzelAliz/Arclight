package io.izzel.arclight.common.mixin.core.commands;

import com.google.common.collect.Maps;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.context.ContextChain;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import io.izzel.arclight.common.bridge.core.commands.CommandSourceStackBridge;
import io.izzel.arclight.common.bridge.core.commands.CommandsBridge;
import io.izzel.arclight.common.bridge.core.server.level.ServerPlayerBridge;
import io.izzel.arclight.common.mod.compat.CommandNodeHooks;
import io.izzel.arclight.common.mod.mixins.annotation.CreateConstructor;
import io.izzel.arclight.common.mod.util.BukkitDispatcher;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import io.papermc.paper.adventure.PaperAdventure;
import io.papermc.paper.command.brigadier.MessageComponentSerializer;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.ExecutionCommandSource;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundCommandsPacket;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Bukkit;
import org.bukkit.event.command.UnknownCommandEvent;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.spigotmc.SpigotConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.function.Function;

@Mixin(Commands.class)
public abstract class CommandsMixin implements CommandsBridge {

    // @formatter:off
    @Shadow public abstract void performCommand(ParseResults<CommandSourceStack> p_242844_, String p_242841_);
    @Shadow public abstract void performPrefixedCommand(CommandSourceStack p_230958_, String p_230959_);
    @Mutable @Shadow @Final private CommandDispatcher<CommandSourceStack> dispatcher;
    @Shadow private static <S> void fillUsableCommands(CommandNode<S> rootCommandSource, CommandNode<S> rootSuggestion, S source, Map<CommandNode<S>, CommandNode<S>> commandNodeToSuggestionNode) {
        throw new AssertionError();
    }
    // @formatter:on

    @CreateConstructor
    public void arclight$constructor() {
        this.dispatcher = new BukkitDispatcher((Commands) (Object) this);
        this.dispatcher.setConsumer(ExecutionCommandSource.resultConsumer());
    }

    public void performPrefixedCommand(CommandSourceStack commandSourceStack, String s, String label) {
        this.performPrefixedCommand(commandSourceStack, s);
    }

    public void performCommand(ParseResults<CommandSourceStack> parseResults, String s, String label) {
        this.performCommand(parseResults, s);
    }

    @Decorate(method = "finishParsing(Lcom/mojang/brigadier/ParseResults;Ljava/lang/String;Lnet/minecraft/commands/CommandSourceStack;)Lcom/mojang/brigadier/context/ContextChain;",
        inject = true, at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/exceptions/CommandSyntaxException;getRawMessage()Lcom/mojang/brigadier/Message;", remap = false),
        require = 1, allow = 1)
    private static void arclight$unknownCommand(ParseResults<CommandSourceStack> command, String commandString, CommandSourceStack sender,
                                                  @Local(ordinal = 0) CommandSyntaxException exception) throws Throwable {
        // This call exists only in finishParsing's syntax-error catch, before either
        // native failure message. Successful parsing and execution failures stay native.
        arclight$sendUnknownCommand(sender, commandString, exception);
        DecorationOps.cancel().invoke((ContextChain<CommandSourceStack>) null);
        return;
    }

    @Unique
    private static void arclight$sendUnknownCommand(CommandSourceStack sender, String commandString, CommandSyntaxException exception) {
        var builder = net.kyori.adventure.text.Component.text()
            .color(net.kyori.adventure.text.format.NamedTextColor.RED)
            .append(MessageComponentSerializer.message().deserialize(exception.getRawMessage()));
        if (exception.getInput() != null && exception.getCursor() >= 0) {
            int cursor = Math.min(exception.getInput().length(), exception.getCursor());
            MutableComponent context = Component.empty()
                .withStyle(ChatFormatting.GRAY)
                .withStyle(style -> style.withClickEvent(new ClickEvent.SuggestCommand("/" + commandString)));
            if (cursor > 10) {
                context.append(CommonComponents.ELLIPSIS);
            }
            context.append(exception.getInput().substring(Math.max(0, cursor - 10), cursor));
            if (cursor < exception.getInput().length()) {
                context.append(Component.literal(exception.getInput().substring(cursor)).withStyle(ChatFormatting.RED, ChatFormatting.UNDERLINE));
            }
            context.append(Component.translatable("command.context.here").withStyle(ChatFormatting.RED, ChatFormatting.ITALIC));
            builder.append(net.kyori.adventure.text.Component.newline()).append(PaperAdventure.asAdventure(context));
        }
        var event = new UnknownCommandEvent((io.papermc.paper.command.brigadier.CommandSourceStack) (Object) sender, commandString,
            SpigotConfig.unknownCommandMessage.isEmpty() ? null : builder.build());
        Bukkit.getPluginManager().callEvent(event);
        var message = event.message();
        if (message != null) {
            // Do not add a red parent style to the listener's replacement component.
            ((CommandSourceStackBridge) sender).bridge$sendFailure(PaperAdventure.asVanilla(message), false);
        }
    }

    /**
     * @author IzzelAliz
     * @reason Keep the loader merge and synchronous Bukkit filtering around Paper's async tree events.
     */
    @Overwrite
    public void sendCommands(ServerPlayer player) {
        if (SpigotConfig.tabComplete < 0) {
            player.connection.send(new ClientboundCommandsPacket(new RootCommandNode<>(), arclight$nodeInspector()));
            return;
        }
        // Snapshot only the root's children before crossing threads. Retain the live
        // root identity as a redirect target, as required by the adopted dispatcher.
        CommandNode<CommandSourceStack> sourceRoot = this.dispatcher.getRoot();
        RootCommandNode<CommandSourceStack> snapshot = new RootCommandNode<>();
        for (CommandNode<CommandSourceStack> child : new java.util.ArrayList<>(sourceRoot.getChildren())) {
            snapshot.addChild(child);
        }
        CommandSourceStack source = player.createCommandSourceStack();
        ((io.izzel.arclight.common.bridge.core.commands.CommandsSendTreeBridge) (Object) this).bridge$sendCommandTree(player, node -> {
            Map<CommandNode<CommandSourceStack>, CommandNode<CommandSourceStack>> map = Maps.newIdentityHashMap();
            map.put(sourceRoot, node);
            map.put(snapshot, node);
            bridge$forge$mergeNode(snapshot, node, map, source, ctx -> 0,
                suggest -> (com.mojang.brigadier.suggestion.SuggestionProvider<CommandSourceStack>) suggest);
        }, (node, labels) -> {
            PlayerCommandSendEvent event = new PlayerCommandSendEvent(((ServerPlayerBridge) player).bridge$getBukkitEntity(), new LinkedHashSet<>(labels));
            Bukkit.getPluginManager().callEvent(event);
            for (String label : labels) {
                if (!event.getCommands().contains(label)) {
                    CommandNodeHooks.removeCommand(node, label);
                }
            }
            player.connection.send(new ClientboundCommandsPacket(bridge$forge$filterCommandTree(player, node), arclight$nodeInspector()));
        });
    }

    private static ClientboundCommandsPacket.NodeInspector<CommandSourceStack> arclight$nodeInspector() {
        CommandSourceStack noPermissionSource = Commands.createCompilationContext(PermissionSet.NO_PERMISSIONS);
        return new ClientboundCommandsPacket.NodeInspector<>() {
            @Override
            public net.minecraft.resources.Identifier suggestionId(com.mojang.brigadier.tree.ArgumentCommandNode<CommandSourceStack, ?> node) {
                SuggestionProvider<CommandSourceStack> provider = node.getCustomSuggestions();
                return provider != null ? SuggestionProviders.getName(provider) : null;
            }

            @Override
            public boolean isExecutable(CommandNode<CommandSourceStack> node) {
                return node.getCommand() != null;
            }

            @Override
            public boolean isRestricted(CommandNode<CommandSourceStack> node) {
                return node.getRequirement() instanceof io.izzel.arclight.common.bridge.core.commands.CommandsBridge.RestrictedMarker
                    || node.getRequirement() != null && !node.getRequirement().test(noPermissionSource);
            }
        };
    }

    @Redirect(method = "fillUsableCommands", at = @At(value = "INVOKE", remap = false, target = "Lcom/mojang/brigadier/tree/CommandNode;canUse(Ljava/lang/Object;)Z"))
    private static <S> boolean arclight$canUse(CommandNode<S> commandNode, S source) {
        return CommandNodeHooks.canUse(commandNode, source);
    }

    @Override
    public <S, T> void bridge$forge$mergeNode(CommandNode<S> sourceNode, CommandNode<T> resultNode,
                                              Map<CommandNode<S>, CommandNode<T>> sourceToResult,
                                              S canUse, Command<T> execute,
                                              Function<SuggestionProvider<S>, SuggestionProvider<T>> sourceToResultSuggestion) {
        fillUsableCommands((CommandNode) sourceNode, (CommandNode) resultNode, canUse, (Map) sourceToResult);
    }
}
