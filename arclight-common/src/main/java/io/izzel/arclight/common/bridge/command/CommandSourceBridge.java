package io.izzel.arclight.common.bridge.command;

import com.mojang.brigadier.tree.CommandNode;
import org.bukkit.command.CommandSender;

public interface CommandSourceBridge {

    CommandNode<?> bridge$getCurrentCommand();

    void bridge$setCurrentCommand(CommandNode<?> node);

    boolean bridge$hasPermission(int i, String bukkitPermission);

    CommandSender bridge$getBukkitSender();
}
