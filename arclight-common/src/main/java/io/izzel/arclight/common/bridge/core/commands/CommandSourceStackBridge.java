package io.izzel.arclight.common.bridge.core.commands;

import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSource;
import net.minecraft.network.chat.Component;
import org.bukkit.command.CommandSender;

public interface CommandSourceStackBridge {

    void bridge$setSource(CommandSource source);

    void bridge$sendFailure(Component message, boolean withStyle);

    CommandNode<?> bridge$getCurrentCommand();

    void bridge$setCurrentCommand(CommandNode<?> node);

    boolean bridge$hasPermission(int i, String bukkitPermission);

    boolean bridge$isBypassSelectorPermissions();

    void bridge$setBypassSelectorPermissions(boolean bypassSelectorPermissions);

    CommandSender bridge$getBukkitSender();

    default org.bukkit.command.CommandSender getBukkitSender() {
        return this.bridge$getBukkitSender();
    }

}
