package io.izzel.arclight.common.mixin.core.commands;

import com.destroystokyo.paper.event.brigadier.AsyncPlayerSendCommandsEvent;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.brigadier.tree.RootCommandNode;
import io.izzel.arclight.common.bridge.core.commands.CommandsSendTreeBridge;
import io.izzel.arclight.common.bridge.core.server.MinecraftServerBridge;
import io.izzel.arclight.common.bridge.core.server.level.ServerPlayerBridge;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Mixin(Commands.class)
public abstract class CommandsSendTreeMixin implements CommandsSendTreeBridge {

    @Unique
    private static final ExecutorService arclight$commandSendingPool = Executors.newFixedThreadPool(2,
        new ThreadFactoryBuilder().setDaemon(true).setNameFormat("Arclight Async Command Builder - %d").build());

    @Override
    public void bridge$sendCommandTree(ServerPlayer player, Consumer<RootCommandNode<CommandSourceStack>> build,
                                      BiConsumer<RootCommandNode<CommandSourceStack>, Collection<String>> send) {
        var root = new RootCommandNode<CommandSourceStack>();
        // Native/mod permission predicates retain their original caller thread.
        build.accept(root);
        arclight$commandSendingPool.execute(() -> {
            Collection<String> labels = new LinkedHashSet<>();
            root.getChildren().forEach(child -> labels.add(child.getName()));
            var bukkitPlayer = ((ServerPlayerBridge) player).bridge$getBukkitEntity();
            arclight$fireSendCommands(bukkitPlayer, root, false);
            ((MinecraftServerBridge) ((org.bukkit.craftbukkit.CraftServer) bukkitPlayer.getServer()).getServer()).bridge$queuedProcess(() -> {
                arclight$fireSendCommands(bukkitPlayer, root, true);
                // Same mutated root reaches the synchronous Bukkit label filter,
                // restricted-node inspector and packet sender in the original owner.
                send.accept(root, labels);
            });
        });
    }

    @Unique
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void arclight$fireSendCommands(org.bukkit.entity.Player player, RootCommandNode<CommandSourceStack> root,
                                                 boolean hasFiredAsync) {
        // The native source implements Paper's API interface after its existing Mixin;
        // Java cannot express that intersection in RootCommandNode's type parameter.
        new AsyncPlayerSendCommandsEvent(player, (RootCommandNode) root, hasFiredAsync).callEvent();
    }
}
