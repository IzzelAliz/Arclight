package io.izzel.arclight.fabric.mod.permission;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.util.TriState;
import org.bukkit.craftbukkit.entity.CraftHumanEntity;
import org.bukkit.permissions.PermissibleBase;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.ServerOperator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ArclightFabricPermissible extends PermissibleBase {

    private final CraftHumanEntity player;

    public ArclightFabricPermissible(@Nullable ServerOperator opable) {
        super(opable);
        this.player = (CraftHumanEntity) opable;
    }

    @Override
    public boolean isPermissionSet(@NotNull String name) {
        return player != null && Permissions.getPermissionValue(player.getHandle(), name) != TriState.DEFAULT;
    }

    @Override
    public boolean isPermissionSet(@NotNull Permission perm) {
        return player != null && Permissions.getPermissionValue(player.getHandle(), perm.getName()) != TriState.DEFAULT;
    }

    @Override
    public boolean hasPermission(@NotNull String name) {
        return player != null && Permissions.check(player.getHandle(), name);
    }

    @Override
    public boolean hasPermission(@NotNull Permission perm) {
        return player != null && Permissions.check(player.getHandle(), perm.getName());
    }
}
