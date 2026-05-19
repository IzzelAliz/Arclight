package io.izzel.arclight.common.mixin.core.commands;

import com.google.common.collect.Maps;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import io.izzel.arclight.common.bridge.core.commands.CommandsBridge;
import io.izzel.arclight.common.bridge.core.server.level.ServerPlayerBridge;
import io.izzel.arclight.common.bridge.core.server.MinecraftServerBridge;
import io.izzel.arclight.common.mod.compat.CommandNodeHooks;
import io.izzel.arclight.common.mod.mixins.annotation.CreateConstructor;
import io.izzel.arclight.common.mod.util.BukkitDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.ExecutionCommandSource;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.network.protocol.game.ClientboundCommandsPacket;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Bukkit;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.spigotmc.SpigotConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
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

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void sendCommands(ServerPlayer player) {
        if (SpigotConfig.tabComplete < 0) return;
        Map<CommandNode<CommandSourceStack>, CommandNode<CommandSourceStack>> map = Maps.newIdentityHashMap();

        RootCommandNode<CommandSourceStack> vanillaRoot = new RootCommandNode<>();
        Commands vanillaCommands = ((MinecraftServerBridge) player.level().getServer()).bridge$getVanillaCommands();
        map.put(vanillaCommands.getDispatcher().getRoot(), vanillaRoot);

        // FORGE: Use our own command node merging method to handle redirect nodes properly, see issue #7551
        bridge$forge$mergeNode(vanillaCommands.getDispatcher().getRoot(), vanillaRoot, map, player.createCommandSourceStack(), ctx -> 0, suggest -> (com.mojang.brigadier.suggestion.SuggestionProvider<CommandSourceStack>) suggest);

        RootCommandNode<CommandSourceStack> node = new RootCommandNode<>();
        map.put(this.dispatcher.getRoot(), node);
        bridge$forge$mergeNode(this.dispatcher.getRoot(), node, map, player.createCommandSourceStack(), ctx -> 0, suggest -> (com.mojang.brigadier.suggestion.SuggestionProvider<CommandSourceStack>) suggest);

        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (CommandNode<CommandSourceStack> child : node.getChildren()) {
            set.add(child.getName());
        }
        PlayerCommandSendEvent event = new PlayerCommandSendEvent(((ServerPlayerBridge) player).bridge$getBukkitEntity(), new LinkedHashSet<>(set));
        Bukkit.getPluginManager().callEvent(event);
        for (String s : set) {
            if (!event.getCommands().contains(s)) {
                CommandNodeHooks.removeCommand(node, s);
            }
        }
        player.connection.send(new ClientboundCommandsPacket(node, arclight$nodeInspector()));
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
                return node.getRequirement() != null && !node.getRequirement().test(noPermissionSource);
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
