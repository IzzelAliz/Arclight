package io.izzel.arclight.bridge.command;

import com.mojang.brigadier.tree.CommandNode;

public interface CommandSourceBridge {

    CommandNode<?> bridge$getCurrentCommand();

    void bridge$setCurrentCommand(CommandNode<?> node);

    boolean bridge$hasPermission(int i, String bukkitPermission);
}
