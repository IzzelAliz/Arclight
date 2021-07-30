package io.izzel.arclight.common.mixin.core.commands;

import com.mojang.brigadier.tree.CommandNode;
import io.izzel.arclight.common.bridge.core.command.CommandSourceBridge;
import io.izzel.arclight.common.bridge.core.command.ICommandSourceBridge;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;
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

@Mixin(CommandSourceStack.class)
public abstract class CommandSourceStackMixin implements CommandSourceBridge {

    // @formatter:off
    @Shadow @Final public CommandSource source;
    @Shadow public abstract ServerLevel getLevel();
    @Shadow @Final private int permissionLevel;
    // @formatter:on

    public CommandNode currentCommand;

    @Inject(method = "hasPermission", cancellable = true, at = @At("HEAD"))
    public void arclight$checkPermission(int level, CallbackInfoReturnable<Boolean> cir) {
        if (currentCommand != null) {
            cir.setReturnValue(hasPermission(level, VanillaCommandWrapper.getPermission(currentCommand)));
        }
    }

    public boolean hasPermission(int i, String bukkitPermission) {
        // World is null when loading functions
        return ((getLevel() == null || !((CraftServer) Bukkit.getServer()).ignoreVanillaPermissions) && this.permissionLevel >= i) || getBukkitSender().hasPermission(bukkitPermission);
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
        return ((ICommandSourceBridge) this.source).bridge$getBukkitSender((CommandSourceStack) (Object) this);
    }

    @Override
    public CommandSender bridge$getBukkitSender() {
        return getBukkitSender();
    }
}
