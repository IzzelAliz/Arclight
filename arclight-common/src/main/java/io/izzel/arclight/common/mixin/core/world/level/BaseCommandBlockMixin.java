package io.izzel.arclight.common.mixin.core.world.level;

import com.google.common.base.Joiner;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.izzel.arclight.common.bridge.core.commands.CommandSourceStackBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BaseCommandBlock;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.event.server.ServerCommandEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BaseCommandBlock.class)
public class BaseCommandBlockMixin {

    @Shadow private boolean trackOutput;

    // Both native execution and Bukkit's direct createSource caller need the owning sender.
    @ModifyExpressionValue(method = "createSource(Lnet/minecraft/server/level/ServerLevel;)Lnet/minecraft/world/level/BaseCommandBlock$CloseableCommandBlockSource;", at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/BaseCommandBlock;trackOutput:Z"))
    private boolean arclight$alwaysCreateSource(boolean original) {
        return true;
    }

    @Inject(method = "createSource(Lnet/minecraft/server/level/ServerLevel;)Lnet/minecraft/world/level/BaseCommandBlock$CloseableCommandBlockSource;", at = @At("RETURN"))
    private void arclight$closeUntrackedSource(ServerLevel level, CallbackInfoReturnable<AutoCloseable> cir) throws Exception {
        if (!this.trackOutput) {
            cir.getReturnValue().close();
        }
    }

    @Decorate(method = "performCommand", at = @At(value = "INVOKE", target = "Lnet/minecraft/commands/Commands;performPrefixedCommand(Lnet/minecraft/commands/CommandSourceStack;Ljava/lang/String;)V"))
    private void arclight$serverCommand(Commands commands, CommandSourceStack sender, String command) throws Throwable {
        Joiner joiner = Joiner.on(" ");
        if (command.startsWith("/")) {
            command = command.substring(1);
        }

        ServerCommandEvent event = new ServerCommandEvent(((CommandSourceStackBridge) sender).bridge$getBukkitSender(), command);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }
        command = event.getCommand();

        String[] args = command.split(" ");

        String cmd = args[0];
        if (cmd.startsWith("minecraft:")) cmd = cmd.substring("minecraft:".length());
        if (cmd.startsWith("bukkit:")) cmd = cmd.substring("bukkit:".length());

        if (cmd.equalsIgnoreCase("stop") || cmd.equalsIgnoreCase("kick") || cmd.equalsIgnoreCase("op")
            || cmd.equalsIgnoreCase("deop") || cmd.equalsIgnoreCase("ban") || cmd.equalsIgnoreCase("ban-ip")
            || cmd.equalsIgnoreCase("pardon") || cmd.equalsIgnoreCase("pardon-ip") || cmd.equalsIgnoreCase("reload")) {
            return;
        }

        if (((CraftServer) Bukkit.getServer()).getCommandBlockOverride(args[0])) {
            args[0] = "minecraft:" + args[0];
        }

        DecorationOps.callsite().invoke(commands, sender, joiner.join(args));
    }
}
