package io.izzel.arclight.common.mod.command;

import com.mojang.brigadier.tree.CommandNode;
import io.izzel.arclight.common.bridge.core.commands.CommandSourceStackBridge;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.server.permissions.Permissions;
import org.bukkit.craftbukkit.command.VanillaCommandWrapper;

import java.util.function.BooleanSupplier;

/** A source-bound Bukkit view; never stored in the stack's raw permissions field. */
public final class CommandPermissionSet implements PermissionSet {

    private final PermissionSet handle;
    private final CommandSourceStackBridge source;
    private final BooleanSupplier allowVanilla;

    public CommandPermissionSet(PermissionSet handle, CommandSourceStackBridge source, BooleanSupplier allowVanilla) {
        this.handle = handle;
        this.source = source;
        this.allowVanilla = allowVanilla;
    }

    public boolean isDelegateFor(PermissionSet permissions) {
        return this.handle == permissions;
    }

    @Override
    public boolean hasPermission(Permission permission) {
        // Evaluate the raw set exactly once, before consulting Bukkit or the current node.
        boolean vanillaPermission = this.handle.hasPermission(permission);
        // CraftBukkit only reads the node's name/redirect; keep the bridge's wildcard ABI.
        @SuppressWarnings("unchecked")
        CommandNode<CommandSourceStack> currentCommand =
            (CommandNode<CommandSourceStack>) this.source.bridge$getCurrentCommand();
        if (currentCommand != null) {
            return hasPermission(vanillaPermission, VanillaCommandWrapper.getPermission(currentCommand));
        }
        if (permission.equals(Permissions.COMMANDS_ENTITY_SELECTORS)) {
            return hasPermission(vanillaPermission, "minecraft.command.selector");
        }
        return vanillaPermission;
    }

    /** Explicit named bridge checks do not substitute the current command's name. */
    public boolean hasPermission(Permission permission, String bukkitPermission) {
        return hasPermission(this.handle.hasPermission(permission), bukkitPermission);
    }

    private boolean hasPermission(boolean vanillaPermission, String bukkitPermission) {
        return (this.allowVanilla.getAsBoolean() && vanillaPermission)
            || this.source.bridge$getBukkitSender().hasPermission(bukkitPermission);
    }
}
