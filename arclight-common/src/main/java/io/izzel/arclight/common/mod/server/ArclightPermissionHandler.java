package io.izzel.arclight.common.mod.server;

import com.mojang.authlib.GameProfile;
import io.izzel.arclight.common.bridge.entity.player.PlayerEntityBridge;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.IPermissionHandler;
import net.minecraftforge.server.permission.context.IContext;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.util.permissions.DefaultPermissions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class ArclightPermissionHandler implements IPermissionHandler {

    public static final ArclightPermissionHandler INSTANCE = new ArclightPermissionHandler();

    private final List<Perm> list = new LinkedList<>();
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    public void initialize() {
        if (!initialized.getAndSet(true)) {
            for (Perm permission : this.list) {
                DefaultPermissions.registerPermission(permission.toBukkit());
            }
            this.list.clear();
        }
    }

    @Override
    public void registerNode(@NotNull String node, @NotNull DefaultPermissionLevel level, @NotNull String desc) {
        PermissionDefault bukkit;
        if (level == DefaultPermissionLevel.ALL) {
            bukkit = PermissionDefault.TRUE;
        } else if (level == DefaultPermissionLevel.OP) {
            bukkit = PermissionDefault.OP;
        } else {
            bukkit = PermissionDefault.FALSE;
        }
        if (initialized.get()) {
            DefaultPermissions.registerPermission(node, desc, bukkit);
        } else {
            this.list.add(new Perm(node, desc, bukkit));
        }
    }

    @Override
    public @NotNull Collection<String> getRegisteredNodes() {
        if (initialized.get()) {
            return Bukkit.getPluginManager().getPermissions().stream().map(Permission::getName).collect(Collectors.toList());
        } else {
            return this.list.stream().map(it -> it.node).collect(Collectors.toList());
        }
    }

    @Override
    public boolean hasPermission(@NotNull GameProfile profile, @NotNull String node, @Nullable IContext context) {
        if (context != null) {
            PlayerEntity player = context.getPlayer();
            if (player != null) {
                return ((PlayerEntityBridge) player).bridge$getBukkitEntity().hasPermission(node);
            }
        }
        Player player = Bukkit.getPlayer(profile.getId());
        if (player != null) {
            return player.hasPermission(node);
        } else {
            Permission perm = Bukkit.getServer().getPluginManager().getPermission(node);
            boolean isOp = ArclightServer.getMinecraftServer().getPlayerList().canSendCommands(profile);
            if (perm != null) {
                return perm.getDefault().getValue(isOp);
            } else {
                return Permission.DEFAULT_PERMISSION.getValue(isOp);
            }
        }
    }

    @Override
    public @NotNull String getNodeDescription(@NotNull String node) {
        if (!initialized.get()) {
            return "";
        } else {
            Permission permission = Bukkit.getPluginManager().getPermission(node);
            return permission == null ? "" : permission.getDescription();
        }
    }

    private static class Perm {

        private final String node;
        private final String desc;
        private final PermissionDefault level;

        public Perm(String node, String desc, PermissionDefault level) {
            this.node = node;
            this.desc = desc;
            this.level = level;
        }

        public Permission toBukkit() {
            return new Permission(node, desc, level);
        }
    }
}
