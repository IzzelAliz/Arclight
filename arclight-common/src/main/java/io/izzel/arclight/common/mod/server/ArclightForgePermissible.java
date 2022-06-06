package io.izzel.arclight.common.mod.server;

import io.izzel.arclight.api.Unsafe;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.permission.PermissionAPI;
import net.minecraftforge.server.permission.handler.IPermissionHandler;
import net.minecraftforge.server.permission.nodes.PermissionDynamicContextKey;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import net.minecraftforge.server.permission.nodes.PermissionType;
import net.minecraftforge.server.permission.nodes.PermissionTypes;
import org.bukkit.craftbukkit.v.entity.CraftHumanEntity;
import org.bukkit.permissions.PermissibleBase;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.ServerOperator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;

public class ArclightForgePermissible extends PermissibleBase {

    private final CraftHumanEntity player;

    public ArclightForgePermissible(@Nullable ServerOperator opable) {
        super(opable);
        this.player = (CraftHumanEntity) opable;
    }

    @Override
    public boolean hasPermission(@NotNull String inName) {
        var node = newNode(inName, (player, playerUUID, context) -> super.hasPermission(inName));
        if (player.getHandle() instanceof ServerPlayer player) {
            return getHandler().getPermission(player, node);
        } else {
            return getHandler().getOfflinePermission(player.getUniqueId(), node);
        }
    }

    @Override
    public boolean hasPermission(@NotNull Permission perm) {
        var node = newNode(perm.getName(), (player, playerUUID, context) -> super.hasPermission(perm));
        if (player.getHandle() instanceof ServerPlayer player) {
            return getHandler().getPermission(player, node);
        } else {
            return getHandler().getOfflinePermission(player.getUniqueId(), node);
        }
    }

    private static final MethodHandle H_handler, H_newNode;

    private static IPermissionHandler getHandler() {
        try {
            return (IPermissionHandler) H_handler.invokeExact();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> PermissionNode<T> newNode(String nodeName, PermissionNode.PermissionResolver<T> defaultResolver, PermissionDynamicContextKey... dynamics) {
        try {
            return (PermissionNode<T>) H_newNode.invokeExact(nodeName, PermissionTypes.BOOLEAN, defaultResolver, dynamics);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    static {
        try {
            H_handler = Unsafe.lookup().findStaticGetter(PermissionAPI.class, "activeHandler", IPermissionHandler.class);
            H_newNode = Unsafe.lookup().findConstructor(PermissionNode.class, MethodType.methodType(void.class, String.class, PermissionType.class, PermissionNode.PermissionResolver.class, PermissionDynamicContextKey[].class));
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}
