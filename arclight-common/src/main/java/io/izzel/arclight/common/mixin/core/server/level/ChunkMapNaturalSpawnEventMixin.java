package io.izzel.arclight.common.mixin.core.server.level;

import com.destroystokyo.paper.event.entity.PlayerNaturallySpawnCreaturesEvent;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.izzel.arclight.common.bridge.core.server.level.NaturalSpawnSelectionBridge;
import io.izzel.arclight.common.bridge.core.server.level.ServerPlayerBridge;
import io.izzel.arclight.common.bridge.core.world.level.WorldBridge;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

@Mixin(ChunkMap.class)
public abstract class ChunkMapNaturalSpawnEventMixin implements NaturalSpawnSelectionBridge {

    @Shadow @Final private ServerLevel level;

    @Unique private final Map<ServerPlayer, PlayerNaturallySpawnCreaturesEvent> arclight$naturalSpawnEvents = new IdentityHashMap<>();
    @Unique private boolean arclight$collectingNaturalSpawnChunks;

    @Override
    public void bridge$clearNaturalSpawnEvents() {
        this.arclight$naturalSpawnEvents.clear();
    }

    @Override
    public void bridge$prepareNaturalSpawnEvents() {
        for (ServerPlayer player : this.level.players()) {
            var bukkitPlayer = ((ServerPlayerBridge) player).bridge$getBukkitEntity();
            int radius = Math.min(((WorldBridge) this.level).bridge$spigotConfig().mobSpawnRange, bukkitPlayer.getViewDistance());
            radius = Math.min(radius, 8);
            var event = new PlayerNaturallySpawnCreaturesEvent(bukkitPlayer, (byte) radius);
            this.arclight$naturalSpawnEvents.put(player, event);
            event.callEvent();
        }
    }

    @WrapMethod(method = "collectSpawningChunks(Ljava/util/List;)V", require = 1, allow = 1)
    private void arclight$naturalSpawnSelection(List<LevelChunk> chunks, Operation<Void> original) {
        boolean previous = this.arclight$collectingNaturalSpawnChunks;
        this.arclight$collectingNaturalSpawnChunks = true;
        try {
            // Preserve the native candidate iterator and NeoForge force-spawn
            // ticket branch. Only the ordinary per-player predicate is edited.
            original.call(chunks);
        } finally {
            this.arclight$collectingNaturalSpawnChunks = previous;
        }
    }

    @WrapMethod(method = "playerIsCloseEnoughForSpawning(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/level/ChunkPos;)Z",
        require = 1, allow = 1)
    private boolean arclight$naturalSpawnPlayer(ServerPlayer player, ChunkPos pos, Operation<Boolean> original) {
        boolean closeEnough = original.call(player, pos);
        if (!this.arclight$collectingNaturalSpawnChunks) {
            return closeEnough;
        }
        var event = this.arclight$naturalSpawnEvents.get(player);
        return closeEnough && event != null && !event.isCancelled();
    }

    @ModifyConstant(method = "playerIsCloseEnoughForSpawning(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/level/ChunkPos;)Z",
        constant = @Constant(doubleValue = 16384.0), require = 1, allow = 1)
    private double arclight$naturalSpawnRadius(double original, ServerPlayer player, ChunkPos pos) {
        if (!this.arclight$collectingNaturalSpawnChunks) {
            return original;
        }
        var event = this.arclight$naturalSpawnEvents.get(player);
        if (event == null || event.isCancelled()) {
            return 0.0;
        }
        int blockRadius = event.getSpawnRadius() << 4;
        return (double) (blockRadius * blockRadius);
    }
}
