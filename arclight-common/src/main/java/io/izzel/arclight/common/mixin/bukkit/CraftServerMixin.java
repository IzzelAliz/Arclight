package io.izzel.arclight.common.mixin.bukkit;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import io.izzel.arclight.common.bridge.bukkit.CraftServerBridge;
import io.izzel.arclight.common.bridge.core.world.WorldBridge;
import io.izzel.arclight.common.mod.server.ArclightServer;
import jline.console.ConsoleReader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.dedicated.DedicatedPlayerList;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.players.PlayerList;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.world.WorldEvent;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.CraftWorld;
import org.bukkit.craftbukkit.v.command.CraftBlockCommandSender;
import org.bukkit.craftbukkit.v.command.CraftCommandMap;
import org.bukkit.craftbukkit.v.entity.CraftEntity;
import org.bukkit.craftbukkit.v.help.SimpleHelpMap;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.SimplePluginManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

@Mixin(CraftServer.class)
public abstract class CraftServerMixin implements CraftServerBridge {

    // @formatter:off
    @Shadow(remap = false) @Final private CraftCommandMap commandMap;
    @Shadow(remap = false) @Final private SimplePluginManager pluginManager;
    @Shadow(remap = false) @Final private SimpleHelpMap helpMap;
    @Shadow(remap = false) protected abstract void enablePlugin(Plugin plugin);
    @Shadow(remap = false) protected abstract void loadCustomPermissions();
    @Shadow(remap = false) @Final protected DedicatedServer console;
    @Shadow(remap = false) @Final @Mutable private String serverName;
    @Shadow(remap = false) @Final @Mutable protected DedicatedPlayerList playerList;
    @Shadow(remap = false) @Final private Map<String, World> worlds;
    @Accessor(value = "logger", remap = false) @Mutable public abstract void setLogger(Logger logger);
    // @formatter:on

    @Inject(method = "<init>", at = @At("RETURN"))
    public void arclight$setBrand(DedicatedServer console, PlayerList playerList, CallbackInfo ci) {
        this.serverName = "Arclight";
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite(remap = false)
    public String getName() {
        return "Arclight";
    }

    @Override
    public void bridge$setPlayerList(PlayerList playerList) {
        this.playerList = (DedicatedPlayerList) playerList;
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite(remap = false)
    public ConsoleReader getReader() {
        return null;
    }

    @Inject(method = "unloadWorld(Lorg/bukkit/World;Z)Z", remap = false, require = 1, at = @At(value = "INVOKE", ordinal = 1, target = "Ljava/util/Map;remove(Ljava/lang/Object;)Ljava/lang/Object;"))
    private void arclight$unloadForge(World world, boolean save, CallbackInfoReturnable<Boolean> cir) {
        MinecraftForge.EVENT_BUS.post(new WorldEvent.Unload(((CraftWorld) world).getHandle()));
        this.console.markWorldsDirty();
    }

    @ModifyVariable(method = "dispatchCommand", remap = false, index = 2, at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lorg/spigotmc/AsyncCatcher;catchOp(Ljava/lang/String;)V"))
    private String arclight$forgeCommandEvent(String commandLine, CommandSender sender) {
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
        CommandEvent event = new CommandEvent(parse);
        if (MinecraftForge.EVENT_BUS.post(event)) {
            return null;
        } else if (event.getException() != null) {
            return null;
        } else {
            String s = event.getParseResults().getReader().getString();
            return s.startsWith("/") ? s.substring(1) : s;
        }
    }

    @Inject(method = "dispatchCommand", remap = false, cancellable = true, at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lorg/spigotmc/AsyncCatcher;catchOp(Ljava/lang/String;)V"))
    private void arclight$returnIfFail(CommandSender sender, String commandLine, CallbackInfoReturnable<Boolean> cir) {
        if (commandLine == null) {
            cir.setReturnValue(false);
        }
    }

    @Override
    public void bridge$removeWorld(ServerLevel world) {
        if (world == null) {
            return;
        }
        this.worlds.remove(((WorldBridge) world).bridge$getWorld().getName().toLowerCase(Locale.ROOT));
    }
}
