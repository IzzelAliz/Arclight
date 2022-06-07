package io.izzel.arclight.common.bridge.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import java.util.Set;

public interface TrackedEntityBridge {

    void bridge$setTrackedPlayers(Set<ServerPlayerEntity> trackedPlayers);

    void bridge$setPassengers(List<Entity> passengers);
}
