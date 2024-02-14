package io.izzel.arclight.fabric.mixin.bukkit;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import io.izzel.arclight.common.mod.server.ArclightServer;
import net.minecraft.commands.CommandResultCallback;
import net.minecraft.commands.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.command.CraftBlockCommandSender;
import org.bukkit.craftbukkit.v.entity.CraftEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = CraftServer.class, remap = false)
public abstract class CraftServerMixin_Fabric {
    @ModifyVariable(method = "dispatchCommand", remap = false, index = 2, at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lorg/spigotmc/AsyncCatcher;catchOp(Ljava/lang/String;)V"))
    private String arclight$forge$forgeCommandEvent(String commandLine, CommandSender sender) {
        CommandSourceStack commandSource;
        if (sender instanceof CraftEntity) {
            commandSource = ((CraftEntity) sender).getHandle().createCommandSourceStack();
        } else if (sender == Bukkit.getConsoleSender()) {
            commandSource = ArclightServer.getMinecraftServer().createCommandSourceStack();
        } else if (sender instanceof CraftBlockCommandSender) {
            commandSource = ((CraftBlockCommandSender) sender).getWrapper();
        } else {
            return commandLine;
        }
        StringReader stringreader = new StringReader("/" + commandLine);
        if (stringreader.canRead() && stringreader.peek() == '/') {
            stringreader.skip();
        }
        ParseResults<CommandSourceStack> parse = ArclightServer.getMinecraftServer().getCommands()
                .getDispatcher().parse(stringreader, commandSource);
        // Todo: Command event.
//        CommandEvent event = new CommandEvent(parse);
//        if (NeoForge.EVENT_BUS.post(event).isCanceled()) {
//            return null;
//        } else if (event.getException() != null) {
//            return null;
//        } else {
//            String s = event.getParseResults().getReader().getString();
//            return s.startsWith("/") ? s.substring(1) : s;
//        }
        var str = parse.getReader().getString();
        return str.startsWith("/") ? str.substring(1) : str;
    }
}
