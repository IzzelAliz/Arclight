package io.izzel.arclight.common.bridge.network.play;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.StringNBT;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.server.ServerWorld;
import org.bukkit.Location;
import org.bukkit.event.player.PlayerTeleportEvent;

// todo
public interface ServerPlayNetHandlerBridge {

    void bridge$pushTeleportCause(PlayerTeleportEvent.TeleportCause cause);

    void bridge$disconnect(String str);

    void bridge$teleport(Location dest);

    boolean bridge$worldNoCollision(ServerWorld world, Entity entity, AxisAlignedBB aabb);

    StringNBT bridge$stringNbt(String s);

    void bridge$dropItems(ServerPlayerEntity player, boolean all);

    boolean bridge$processedDisconnect();

    boolean bridge$isDisconnected();
}
