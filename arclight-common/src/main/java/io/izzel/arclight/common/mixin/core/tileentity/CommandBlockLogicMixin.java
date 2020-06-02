package io.izzel.arclight.common.mixin.core.tileentity;

import com.google.common.base.Joiner;
import io.izzel.arclight.common.bridge.command.CommandSourceBridge;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.tileentity.CommandBlockLogic;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.event.server.ServerCommandEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CommandBlockLogic.class)
public class CommandBlockLogicMixin {

    // @formatter:off
    @Shadow private ITextComponent customName;
    // @formatter:on

    @Redirect(method = "trigger", at = @At(value = "INVOKE", target = "Lnet/minecraft/command/Commands;handleCommand(Lnet/minecraft/command/CommandSource;Ljava/lang/String;)I"))
    private int arclight$serverCommand(Commands commands, CommandSource sender, String command) {
        Joiner joiner = Joiner.on(" ");
        if (command.startsWith("/")) {
            command = command.substring(1);
        }

        ServerCommandEvent event = new ServerCommandEvent(((CommandSourceBridge) sender).bridge$getBukkitSender(), command);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return 0;
        }
        command = event.getCommand();

        String[] args = command.split(" ");

        String cmd = args[0];
        if (cmd.startsWith("minecraft:")) cmd = cmd.substring("minecraft:".length());
        if (cmd.startsWith("bukkit:")) cmd = cmd.substring("bukkit:".length());

        if (cmd.equalsIgnoreCase("stop") || cmd.equalsIgnoreCase("kick") || cmd.equalsIgnoreCase("op")
            || cmd.equalsIgnoreCase("deop") || cmd.equalsIgnoreCase("ban") || cmd.equalsIgnoreCase("ban-ip")
            || cmd.equalsIgnoreCase("pardon") || cmd.equalsIgnoreCase("pardon-ip") || cmd.equalsIgnoreCase("reload")) {
            return 0;
        }

        if (((CraftServer) Bukkit.getServer()).getCommandBlockOverride(args[0])) {
            args[0] = "minecraft:" + args[0];
        }

        return commands.handleCommand(sender, joiner.join(args));
    }

    @Inject(method = "setName", at = @At("RETURN"))
    public void arclight$setName(ITextComponent nameIn, CallbackInfo ci) {
        if (this.customName == null) {
            this.customName = new StringTextComponent("@");
        }
    }
}
