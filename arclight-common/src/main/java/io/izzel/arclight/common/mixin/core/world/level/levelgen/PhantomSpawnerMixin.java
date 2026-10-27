package io.izzel.arclight.common.mixin.core.world.level.levelgen;

import com.destroystokyo.paper.event.entity.PhantomPreSpawnEvent;
import io.izzel.arclight.common.bridge.core.server.level.ServerLevelBridge;
import io.izzel.arclight.common.bridge.core.server.level.ServerPlayerBridge;
import io.izzel.arclight.common.bridge.core.world.level.WorldBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.PhantomSpawner;
import org.bukkit.craftbukkit.util.CraftLocation;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PhantomSpawner.class)
public class PhantomSpawnerMixin {

    @Decorate(method = "tick(Lnet/minecraft/server/level/ServerLevel;Z)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityType;create(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/EntitySpawnReason;)Lnet/minecraft/world/entity/Entity;"),
        require = 1, allow = 1)
    private Entity arclight$phantomPreSpawn(EntityType<?> type, Level level, EntitySpawnReason reason,
                                           @Local(ordinal = 0) ServerPlayer player,
                                           @Local(ordinal = -1) BlockPos spawnPos,
                                           @Local(ordinal = -2) int groupSize,
                                           @Local(ordinal = -1) int index) throws Throwable {
        var event = new PhantomPreSpawnEvent(
            CraftLocation.toBukkit(spawnPos, ((WorldBridge) level).bridge$getWorld()),
            ((ServerPlayerBridge) player).bridge$getBukkitEntity(), CreatureSpawnEvent.SpawnReason.NATURAL);
        if (!event.callEvent()) {
            if (event.shouldAbortSpawn()) {
                // The native null branch advances the index and then exits this
                // player's group, without skipping the remaining players.
                groupSize = index;
                DecorationOps.blackhole().invoke(groupSize);
            }
            return null;
        }
        return (Entity) DecorationOps.callsite().invoke(type, level, reason);
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)V"))
    private void arclight$addSpawnReason(ServerLevel level, boolean spawnEnemies, CallbackInfo ci) {
        ((ServerLevelBridge)level).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.NATURAL);
    }
}
