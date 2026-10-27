package io.izzel.arclight.common.mixin.core.commands;

import com.mojang.brigadier.tree.CommandNode;
import io.izzel.arclight.common.bridge.core.commands.CommandSourceStackBridge;
import io.izzel.arclight.common.bridge.core.command.CommandSourceBridge;
import io.izzel.arclight.common.bridge.core.server.level.ServerPlayerBridge;

import io.izzel.arclight.common.mod.command.CommandPermissionSet;
import io.izzel.arclight.common.mod.compat.CommandNodeHooks;
import io.izzel.arclight.common.mod.server.command.ArclightDummyCommandSender;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.server.permissions.PermissionSet;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.CraftServer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.Objects;

@Mixin(CommandSourceStack.class)
public abstract class CommandSourceStackMixin implements CommandSourceStackBridge, io.papermc.paper.command.brigadier.PaperCommandSourceStack {

    @Override
    public CommandSourceStack getHandle() {
        return (CommandSourceStack) (Object) this;
    }

    @Override
    public io.papermc.paper.command.brigadier.CommandSourceStack withLocation(org.bukkit.Location location) {
        Objects.requireNonNull(location, "Location cannot be null");
        var world = (org.bukkit.craftbukkit.CraftWorld) Objects.requireNonNull(location.getWorld(), "World cannot be null");
        return this.getHandle().withLevel(world.getHandle())
            .withPosition(new net.minecraft.world.phys.Vec3(location.getX(), location.getY(), location.getZ()))
            .withRotation(new net.minecraft.world.phys.Vec2(location.getPitch(), location.getYaw()));
    }

    // @formatter:off
    @Shadow @Final @Mutable public CommandSource source;
    @Shadow public abstract ServerLevel getLevel();
    @Shadow @Final private PermissionSet permissions;
    @Shadow public abstract void sendFailure(Component message);
    @Shadow public abstract boolean isSilent();
    // @formatter:on

    @Override
    public void bridge$setSource(CommandSource source) {
        this.source = source;
    }

    @Override
    public void bridge$sendFailure(Component message, boolean withStyle) {
        if (withStyle) {
            this.sendFailure(message);
        } else if (this.source.acceptsFailure() && !this.isSilent()) {
            this.source.sendSystemMessage(message);
        }
    }

    public CommandNode currentCommand;

    @Unique private CommandPermissionSet arclight$bukkitPermissions;
    public boolean bypassSelectorPermissions;

    @Override
    public boolean bridge$isBypassSelectorPermissions() {
        return this.bypassSelectorPermissions;
    }

    @Override
    public void bridge$setBypassSelectorPermissions(boolean bypassSelectorPermissions) {
        this.bypassSelectorPermissions = bypassSelectorPermissions;
    }

    @Inject(method = "permissions()Lnet/minecraft/server/permissions/PermissionSet;", cancellable = true, at = @At("RETURN"))
    private void arclight$permissions(CallbackInfoReturnable<PermissionSet> cir) {
        cir.setReturnValue(arclight$permissionView(cir.getReturnValue()));
    }

    @Unique
    private CommandPermissionSet arclight$permissionView(PermissionSet permissions) {
        if (this.arclight$bukkitPermissions == null || !this.arclight$bukkitPermissions.isDelegateFor(permissions)) {
            this.arclight$bukkitPermissions = new CommandPermissionSet(permissions, this,
                () -> getLevel() == null || !((CraftServer) Bukkit.getServer()).ignoreVanillaPermissions);
        }
        return this.arclight$bukkitPermissions;
    }

    @Redirect(method = "broadcastToAdmins(Lnet/minecraft/network/chat/Component;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;isOp(Lnet/minecraft/server/players/NameAndId;)Z"))
    private boolean arclight$feedbackPermission(PlayerList instance, NameAndId nameAndId) {
        if (nameAndId.id() == null) {
            return false;
        }
        var player = instance.getPlayer(nameAndId.id());
        return player != null && ((ServerPlayerBridge) player).bridge$getBukkitEntity().hasPermission("minecraft.admin.command_feedback");
    }

    public boolean hasPermission(int i, String bukkitPermission) {
        return arclight$permissionView(this.permissions).hasPermission(
            new Permission.HasCommandLevel(PermissionLevel.byId(i)), bukkitPermission);
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
