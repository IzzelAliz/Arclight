package io.izzel.arclight.mixin.core.command;

import com.mojang.brigadier.tree.CommandNode;
import io.izzel.arclight.bridge.command.CommandSourceBridge;
import io.izzel.arclight.bridge.command.ICommandSourceBridge;
import net.minecraft.command.CommandSource;
import net.minecraft.command.ICommandSource;
import net.minecraft.world.server.ServerWorld;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.command.VanillaCommandWrapper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CommandSource.class)
public abstract class CommandSourceMixin implements CommandSourceBridge {

    // @formatter:off
    @Shadow @Final public ICommandSource source;
    @Shadow public abstract ServerWorld func_197023_e();
    @Shadow @Final private int permissionLevel;
    // @formatter:on

    public CommandNode currentCommand;

    @Inject(method = "hasPermissionLevel", cancellable = true, at = @At("HEAD"))
    public void arclight$checkPermission(int level, CallbackInfoReturnable<Boolean> cir) {
        if (currentCommand != null) {
            cir.setReturnValue(hasPermission(level, VanillaCommandWrapper.getPermission(currentCommand)));
        }
    }

    public boolean hasPermission(int i, String bukkitPermission) {
        // World is null when loading functions
        return ((func_197023_e() == null || !((CraftServer) Bukkit.getServer()).ignoreVanillaPermissions) && this.permissionLevel >= i) || getBukkitSender().hasPermission(bukkitPermission);
    }

    @Override
    public boolean bridge$hasPermission(int i, String bukkitPermission) {
        return hasPermission(i, bukkitPermission);
    }

    @Override
    public CommandNode<?> bridge$getCurrentCommand() {
        return currentCommand;
    }

    @Override
    public void bridge$setCurrentCommand(CommandNode<?> node) {
        this.currentCommand = node;
    }

    public CommandSender getBukkitSender() {
        return ((ICommandSourceBridge) this.source).bridge$getBukkitSender((CommandSource) (Object) this);
    }
}
