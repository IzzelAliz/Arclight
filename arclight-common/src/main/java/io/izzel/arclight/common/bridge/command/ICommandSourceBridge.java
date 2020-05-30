package io.izzel.arclight.common.bridge.command;

import net.minecraft.command.CommandSource;
import org.bukkit.command.CommandSender;

public interface ICommandSourceBridge {

    CommandSender bridge$getBukkitSender(CommandSource wrapper);
}
