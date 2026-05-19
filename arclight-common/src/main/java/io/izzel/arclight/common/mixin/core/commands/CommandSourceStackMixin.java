package io.izzel.arclight.common.mixin.core.commands;

import com.mojang.brigadier.tree.CommandNode;
import io.izzel.arclight.common.bridge.core.commands.CommandSourceStackBridge;
import io.izzel.arclight.common.bridge.core.command.CommandSourceBridge;

import io.izzel.arclight.common.mod.compat.CommandNodeHooks;
import io.izzel.arclight.common.mod.server.command.ArclightDummyCommandSender;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.server.permissions.PermissionSet;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.command.VanillaCommandWrapper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.Objects;

@Mixin(CommandSourceStack.class)
public abstract class CommandSourceStackMixin implements CommandSourceStackBridge {

    // @formatter:off
    @Shadow @Final @Mutable public CommandSource source;
    @Shadow public abstract ServerLevel getLevel();
    @Shadow @Final private PermissionSet permissions;
    // @formatter:on

    @Override
    public void bridge$setSource(CommandSource source) {
        this.source = source;
    }

    public CommandNode currentCommand;

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Inject(method = "hasPermission", cancellable = true, at = @At("HEAD"))
    public void arclight$checkPermission(int level, String permission, CallbackInfoReturnable<Boolean> cir) {
        CommandNode currentCommand = bridge$getCurrentCommand();
        if (currentCommand != null) {
            cir.setReturnValue(hasPermission(level, VanillaCommandWrapper.getPermission(currentCommand)));
        }
    }

    @Redirect(method = "sendSuccess", at = @At(value = "INVOKE", target = "Lnet/minecraft/commands/CommandSource;shouldInformAdmins()Z"))
    private boolean arclight$feedbackPermission(CommandSource instance) {
        return instance.shouldInformAdmins() && getBukkitSender().hasPermission("minecraft.admin.command_feedback");
    }

    public boolean hasPermission(int i, String bukkitPermission) {
        // World is null when loading functions
        return ((getLevel() == null || !((CraftServer) Bukkit.getServer()).ignoreVanillaPermissions) && this.permissions.hasPermission(new Permission.HasCommandLevel(PermissionLevel.byId(i)))) || getBukkitSender().hasPermission(bukkitPermission);
    }

    @Override
    public boolean bridge$hasPermission(int i, String bukkitPermission) {
        return hasPermission(i, bukkitPermission);
    }

    @Override
    public CommandNode<?> bridge$getCurrentCommand() {
        if (currentCommand == null) {
            return CommandNodeHooks.getCurrent();
        } else {
            return currentCommand;
        }
    }

    @Override
    public void bridge$setCurrentCommand(CommandNode<?> node) {
        this.currentCommand = node;
    }

    public CommandSender getBukkitSender() {
        var thus = (CommandSourceStack) (Object) this;
        var sender = ((CommandSourceBridge) this.source).bridge$getBukkitSender(thus);
        // It means that this is a custom CommandSource
        return Objects.requireNonNullElseGet(sender, () -> new ArclightDummyCommandSender(thus));
    }

    @Override
    public CommandSender bridge$getBukkitSender() {
        return getBukkitSender();
    }
}
