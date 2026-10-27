package io.izzel.arclight.common.bridge.core.commands;

import com.mojang.brigadier.tree.RootCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface CommandsSendTreeBridge {

    void bridge$sendCommandTree(ServerPlayer player, Consumer<RootCommandNode<CommandSourceStack>> build,
                               BiConsumer<RootCommandNode<CommandSourceStack>, Collection<String>> send);
}
