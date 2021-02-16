package io.izzel.arclight.common.bridge.util;

import io.izzel.arclight.common.bridge.world.WorldBridge;
import net.minecraft.util.IWorldPosCallable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v.CraftWorld;

public interface IWorldPosCallableBridge {

    default World bridge$getWorld() {
        return ((IWorldPosCallable) this).apply((a, b) -> a).orElse(null);
    }

    default BlockPos bridge$getPosition() {
        return ((IWorldPosCallable) this).apply((a, b) -> b).orElse(null);
    }

    default Location bridge$getLocation() {
        CraftWorld world = ((WorldBridge) bridge$getWorld()).bridge$getWorld();
        BlockPos blockPos = bridge$getPosition();
        if (blockPos == null) {
            return null;
        } else {
            return new Location(world, blockPos.getX(), blockPos.getY(), blockPos.getZ());
        }
    }
}
