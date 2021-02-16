package io.izzel.arclight.common.mixin.core.command;

import com.google.common.collect.Maps;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import io.izzel.arclight.common.bridge.command.CommandNodeBridge;
import io.izzel.arclight.common.bridge.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.server.MinecraftServerBridge;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.server.SCommandListPacket;
import org.bukkit.Bukkit;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.spigotmc.SpigotConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.LinkedHashSet;
import java.util.Map;

@Mixin(Commands.class)
public abstract class CommandsMixin {

    // @formatter:off
    @Shadow public abstract int handleCommand(CommandSource source, String command);
    @Mutable @Shadow @Final private CommandDispatcher<CommandSource> dispatcher;
    @Shadow protected abstract void commandSourceNodesToSuggestionNodes(CommandNode<CommandSource> rootCommandSource, CommandNode<ISuggestionProvider> rootSuggestion, CommandSource source, Map<CommandNode<CommandSource>, CommandNode<ISuggestionProvider>> commandNodeToSuggestionNode);
    // @formatter:on

    public void arclight$constructor() {
        this.dispatcher = new CommandDispatcher<>();
        this.dispatcher.setConsumer((context, b, i) -> context.getSource().onCommandComplete(context, b, i));
    }

    public int a(CommandSource source, String command, String label) {
        return this.handleCommand(source, command);
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void send(ServerPlayerEntity player) {
        if (SpigotConfig.tabComplete < 0) return;
        Map<CommandNode<CommandSource>, CommandNode<ISuggestionProvider>> map = Maps.newIdentityHashMap();

        RootCommandNode<ISuggestionProvider> vanillaRoot = new RootCommandNode<>();
        Commands vanillaCommands = ((MinecraftServerBridge) player.server).bridge$getVanillaCommands();
        map.put(vanillaCommands.getDispatcher().getRoot(), vanillaRoot);
        this.commandSourceNodesToSuggestionNodes(vanillaCommands.getDispatcher().getRoot(), vanillaRoot, player.getCommandSource(), map);

        RootCommandNode<ISuggestionProvider> node = new RootCommandNode<>();
        map.put(this.dispatcher.getRoot(), node);
        this.commandSourceNodesToSuggestionNodes(this.dispatcher.getRoot(), node, player.getCommandSource(), map);

        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (CommandNode<ISuggestionProvider> child : node.getChildren()) {
            set.add(child.getName());
        }
        PlayerCommandSendEvent event = new PlayerCommandSendEvent(((ServerPlayerEntityBridge) player).bridge$getBukkitEntity(), new LinkedHashSet<>(set));
        Bukkit.getPluginManager().callEvent(event);
        for (String s : set) {
            if (!event.getCommands().contains(s)) {
                ((CommandNodeBridge) node).bridge$removeCommand(s);
            }
        }
        player.connection.sendPacket(new SCommandListPacket(node));
    }
}
