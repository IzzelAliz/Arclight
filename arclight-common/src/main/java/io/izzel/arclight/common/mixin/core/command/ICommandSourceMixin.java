package io.izzel.arclight.common.mixin.core.command;

import io.izzel.arclight.common.bridge.command.ICommandSourceBridge;
import net.minecraft.command.CommandSource;
import net.minecraft.command.ICommandSource;
import org.bukkit.command.CommandSender;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ICommandSource.class)
public interface ICommandSourceMixin extends ICommandSourceBridge {

    default CommandSender getBukkitSender(CommandSource wrapper) {
        return this.bridge$getBukkitSender(wrapper);
    }
}
