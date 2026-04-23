package io.izzel.arclight.common.bridge.core.commands;

import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSource;
import org.bukkit.command.CommandSender;

public interface CommandSourceStackBridge {

    void bridge$setSource(CommandSource source);

    CommandNode<?> bridge$getCurrentCommand();

    void bridge$setCurrentCommand(CommandNode<?> node);

    boolean bridge$hasPermission(int i, String bukkitPermission);

    CommandSender bridge$getBukkitSender();
}
