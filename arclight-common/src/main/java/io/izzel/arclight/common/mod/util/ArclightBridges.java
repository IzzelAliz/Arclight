package io.izzel.arclight.common.mod.util;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.server.level.ServerPlayerBridge;
import io.izzel.arclight.common.bridge.core.world.level.WorldBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.entity.CraftPlayer;

public final class ArclightBridges {

    private ArclightBridges() {
    }

    public static CraftWorld toCraftWorld(Level level) {
        return ((WorldBridge) level).bridge$getWorld();
    }

    public static CraftWorld toCraftWorld(ServerLevel level) {
        return ((WorldBridge) level).bridge$getWorld();
    }

    public static CraftEntity toBukkit(Entity entity) {
        return ((EntityBridge) entity).bridge$getBukkitEntity();
    }

    public static CraftPlayer toBukkit(ServerPlayer player) {
        return ((ServerPlayerBridge) player).bridge$getBukkitEntity();
    }
}
