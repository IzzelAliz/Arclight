package io.izzel.arclight.common.mixin.core.commands;

import io.izzel.arclight.common.bridge.core.command.CommandSourceBridge;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionSet;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v.command.ServerCommandSender;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(targets = "net/minecraft/commands/CommandSource$1")
public class CommandSource1Mixin implements CommandSourceBridge {


    public CommandSender getBukkitSender(CommandSourceStack wrapper) {
        return new ServerCommandSender() {
            // Compilation contexts used to inspect command trees deliberately have no server.
            private final boolean isOp = wrapper.getServer() != null && wrapper.permissions().hasPermission(new Permission.HasCommandLevel(wrapper.getServer().operatorUserPermissions().level()));

            @Override
            public boolean isOp() {
                return isOp;
            }

            @Override
            public void setOp(boolean value) {
            }

            @Override
            public void sendMessage(@NotNull String message) {

            }

            @Override
            public void sendMessage(@NotNull String[] messages) {

            }

            @NotNull
            @Override
            public String getName() {
                return "NULL";
            }
        };
    }

    @Override
    public CommandSender bridge$getBukkitSender(CommandSourceStack wrapper) {
        return getBukkitSender(wrapper);
    }
}
