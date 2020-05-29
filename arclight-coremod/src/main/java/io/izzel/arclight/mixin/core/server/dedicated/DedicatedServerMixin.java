package io.izzel.arclight.mixin.core.server.dedicated;

import io.izzel.arclight.mixin.core.server.MinecraftServerMixin;
import io.izzel.arclight.mod.server.BukkitRegistry;
import net.minecraft.network.rcon.RConConsoleSource;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.util.text.ITextComponent;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.command.CraftRemoteConsoleCommandSender;
import org.bukkit.plugin.PluginLoadOrder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DedicatedServer.class)
public abstract class DedicatedServerMixin extends MinecraftServerMixin {

    // @formatter:off
    @Shadow @Final public RConConsoleSource rconConsoleSource;
    // @formatter:on

    @Inject(method = "init", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/server/dedicated/DedicatedServer;setPlayerList(Lnet/minecraft/server/management/PlayerList;)V"))
    public void arclight$loadPlugins(CallbackInfoReturnable<Boolean> cir) {
        BukkitRegistry.unlockRegistries();
        ((CraftServer) Bukkit.getServer()).loadPlugins();
        ((CraftServer) Bukkit.getServer()).enablePlugins(PluginLoadOrder.STARTUP);
        BukkitRegistry.lockRegistries();
    }

    @Inject(method = "init", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/network/rcon/MainThread;startThread()V"))
    public void arclight$setRcon(CallbackInfoReturnable<Boolean> cir) {
        this.remoteConsole = new CraftRemoteConsoleCommandSender(this.rconConsoleSource);
    }

    @Inject(method = "sendMessage", cancellable = true, at = @At("HEAD"))
    public void arclight$consoleLog(ITextComponent message, CallbackInfo ci) {
        Bukkit.getConsoleSender().sendMessage(message.getFormattedText());
        ci.cancel();
    }

    @Inject(method = "systemExitNow", at = @At("RETURN"))
    public void arclight$exitNow(CallbackInfo ci) {
        new Thread(() -> {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ignored) {
            } finally {
                Runtime.getRuntime().halt(0);
            }
        }, "Exit Thread").start();
        System.exit(0);
    }
}
