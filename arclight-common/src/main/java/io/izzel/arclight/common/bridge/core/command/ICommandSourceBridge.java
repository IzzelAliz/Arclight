package io.izzel.arclight.common.bridge.core.command;

import net.minecraft.commands.CommandSourceStack;
import org.bukkit.command.CommandSender;

public interface ICommandSourceBridge {

    CommandSender bridge$getBukkitSender(CommandSourceStack wrapper);
}
