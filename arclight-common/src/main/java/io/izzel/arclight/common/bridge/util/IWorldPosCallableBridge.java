package io.izzel.arclight.common.bridge.util;

import io.izzel.arclight.common.bridge.world.WorldBridge;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.bukkit.Location;

public interface IWorldPosCallableBridge {

    World bridge$getWorld();

    BlockPos bridge$getPosition();

    default Location bridge$getLocation() {
        return new Location(((WorldBridge) bridge$getWorld()).bridge$getWorld(), bridge$getPosition().getX(), bridge$getPosition().getY(), bridge$getPosition().getZ());
    }
}
