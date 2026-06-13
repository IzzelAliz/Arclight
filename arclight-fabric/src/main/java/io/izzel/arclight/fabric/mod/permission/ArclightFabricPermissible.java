package io.izzel.arclight.fabric.mod.permission;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.server.level.ServerPlayer;
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

    private @Nullable ServerPlayer nmsPlayer() {
        return player != null ? (ServerPlayer) player.getHandle() : null;
    }

    @Override
    public boolean isPermissionSet(@NotNull String name) {
        var nms = nmsPlayer();
        return nms != null && Permissions.getPermissionValue(nms, name) != TriState.DEFAULT;
    }

    @Override
    public boolean isPermissionSet(@NotNull Permission perm) {
        var nms = nmsPlayer();
        return nms != null && Permissions.getPermissionValue(nms, perm.getName()) != TriState.DEFAULT;
    }

    @Override
    public boolean hasPermission(@NotNull String name) {
        var nms = nmsPlayer();
        return nms != null && Permissions.check(nms, name);
    }

    @Override
    public boolean hasPermission(@NotNull Permission perm) {
        var nms = nmsPlayer();
        return nms != null && Permissions.check(nms, perm.getName());
    }
}
