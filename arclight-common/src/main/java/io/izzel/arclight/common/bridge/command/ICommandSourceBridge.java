package io.izzel.arclight.common.bridge.command;

import net.minecraft.command.CommandSource;
import net.minecraft.command.ICommandSource;
import org.bukkit.command.CommandSender;

public interface ICommandSourceBridge {

    default CommandSender bridge$getBukkitSender(CommandSource wrapper) {
        return ((ICommandSourceBridge) ICommandSource.DUMMY).bridge$getBukkitSender(wrapper);
    }
}
