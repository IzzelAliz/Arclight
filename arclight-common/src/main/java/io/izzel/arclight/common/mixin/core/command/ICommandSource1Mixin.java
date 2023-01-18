package io.izzel.arclight.common.mixin.core.command;

import io.izzel.arclight.common.bridge.command.ICommandSourceBridge;
import net.minecraft.command.CommandSource;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v.command.ServerCommandSender;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(targets = "net/minecraft/command/ICommandSource$1")
public class ICommandSource1Mixin implements ICommandSourceBridge {


    public CommandSender getBukkitSender(CommandSource wrapper) {
        return new ServerCommandSender() {
            private Boolean isOp = null;

            @Override
            public boolean isOp() {
                if (isOp == null) {
                    isOp = wrapper.hasPermissionLevel(wrapper.getServer().getOpPermissionLevel());
                }
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
                return "DUMMY";
            }
        };
    }

    @Override
    public CommandSender bridge$getBukkitSender(CommandSource wrapper) {
        return getBukkitSender(wrapper);
    }
}
