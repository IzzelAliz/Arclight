package io.izzel.arclight.common.mixin.core.commands;

import com.google.common.collect.Maps;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.core.server.MinecraftServerBridge;
import io.izzel.arclight.common.mod.compat.CommandNodeHooks;
import io.izzel.arclight.common.mod.util.BukkitDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.protocol.game.ClientboundCommandsPacket;
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

@Mixin(Commands.class)
public abstract class CommandsMixin {

    // @formatter:off
    @Shadow public abstract int performCommand(CommandSourceStack source, String command);
    @Mutable @Shadow @Final private CommandDispatcher<CommandSourceStack> dispatcher;
    @Shadow protected abstract void fillUsableCommands(CommandNode<CommandSourceStack> rootCommandSource, CommandNode<SharedSuggestionProvider> rootSuggestion, CommandSourceStack source, Map<CommandNode<CommandSourceStack>, CommandNode<SharedSuggestionProvider>> commandNodeToSuggestionNode);
    // @formatter:on

    public void arclight$constructor() {
        this.dispatcher = new BukkitDispatcher((Commands) (Object) this);
        this.dispatcher.setConsumer((context, b, i) -> context.getSource().onCommandComplete(context, b, i));
    }

    public int performCommand(CommandSourceStack source, String command, String label, boolean strip) {
        return this.performCommand(source, command);
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void sendCommands(ServerPlayer player) {
        if (SpigotConfig.tabComplete < 0) return;
        Map<CommandNode<CommandSourceStack>, CommandNode<SharedSuggestionProvider>> map = Maps.newIdentityHashMap();

        RootCommandNode<SharedSuggestionProvider> vanillaRoot = new RootCommandNode<>();
        Commands vanillaCommands = ((MinecraftServerBridge) player.server).bridge$getVanillaCommands();
        map.put(vanillaCommands.getDispatcher().getRoot(), vanillaRoot);
        this.fillUsableCommands(vanillaCommands.getDispatcher().getRoot(), vanillaRoot, player.createCommandSourceStack(), map);

        RootCommandNode<SharedSuggestionProvider> node = new RootCommandNode<>();
        map.put(this.dispatcher.getRoot(), node);
        this.fillUsableCommands(this.dispatcher.getRoot(), node, player.createCommandSourceStack(), map);

        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (CommandNode<SharedSuggestionProvider> child : node.getChildren()) {
            set.add(child.getName());
        }
        PlayerCommandSendEvent event = new PlayerCommandSendEvent(((ServerPlayerEntityBridge) player).bridge$getBukkitEntity(), new LinkedHashSet<>(set));
        Bukkit.getPluginManager().callEvent(event);
        for (String s : set) {
            if (!event.getCommands().contains(s)) {
                CommandNodeHooks.removeCommand(node, s);
            }
        }
        player.connection.send(new ClientboundCommandsPacket(node));
    }

    @Redirect(method = "fillUsableCommands", at = @At(value = "INVOKE", remap = false, target = "Lcom/mojang/brigadier/tree/CommandNode;canUse(Ljava/lang/Object;)Z"))
    private <S> boolean arclight$canUse(CommandNode<S> commandNode, S source) {
        return CommandNodeHooks.canUse(commandNode, source);
    }
}
