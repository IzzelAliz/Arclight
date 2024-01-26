package io.izzel.arclight.forge.mod.permission;

import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.permission.handler.IPermissionHandler;
import net.minecraftforge.server.permission.nodes.PermissionDynamicContext;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import net.minecraftforge.server.permission.nodes.PermissionTypes;
import org.bukkit.Bukkit;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class ArclightPermissionHandler implements IPermissionHandler {

    private final IPermissionHandler delegate;

    public ArclightPermissionHandler(IPermissionHandler delegate) {
        Objects.requireNonNull(delegate, "permission handler");
        this.delegate = delegate;
    }

    @Override
    public ResourceLocation getIdentifier() {
        return new ResourceLocation("arclight", "permission");
    }

    @Override
    public Set<PermissionNode<?>> getRegisteredNodes() {
        return delegate.getRegisteredNodes();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getPermission(ServerPlayer player, PermissionNode<T> node, PermissionDynamicContext<?>... context) {
        if (node.getType() == PermissionTypes.BOOLEAN) {
            return (T) (Object) ((ServerPlayerEntityBridge) player).bridge$getBukkitEntity().hasPermission(node.getNodeName());
        } else {
            return delegate.getPermission(player, node, context);
        }
    }

    @Override
    public <T> T getOfflinePermission(UUID uuid, PermissionNode<T> node, PermissionDynamicContext<?>... context) {
        var player = Bukkit.getPlayer(uuid);
        if (player != null && node.getType() == PermissionTypes.BOOLEAN) {
            return (T) (Object) player.hasPermission(node.getNodeName());
        } else {
            return delegate.getOfflinePermission(uuid, node, context);
        }
    }
}
