package io.izzel.arclight.common.mixin.optimization.general.network;

import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.core.world.server.ChunkMap_TrackedEntityBridge;
import io.izzel.arclight.common.mod.compat.ModIds;
import io.izzel.arclight.common.mod.mixins.annotation.LoadIfMod;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerPlayerConnection;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;

@Mixin(ChunkMap.class)
@LoadIfMod(modid = ModIds.IMMERSIVE_PORTALS, condition = LoadIfMod.ModCondition.ABSENT)
public class ChunkMapMixin_Optimize {

    // @formatter:off
    @Shadow @Final public Int2ObjectMap<ChunkMap.TrackedEntity> entityMap;
    @Shadow @Final public ServerLevel level;
    // @formatter:on

    @Redirect(method = "move", at = @At(value = "INVOKE", remap = false, target = "Lit/unimi/dsi/fastutil/ints/Int2ObjectMap;values()Lit/unimi/dsi/fastutil/objects/ObjectCollection;"))
    private ObjectCollection<ChunkMap.TrackedEntity> arclight$markDirty(Int2ObjectMap<ChunkMap.TrackedEntity> instance, ServerPlayer player) {
        ((ServerPlayerEntityBridge) player).bridge$setTrackerDirty(true);
        return new ObjectArraySet<>();
    }

    @Inject(method = "tick()V", cancellable = true, at = @At("HEAD"))
    private void arclight$optimizedTick(CallbackInfo ci) {
        var list = new ArrayList<ChunkMap.TrackedEntity>(this.level.players().size());

        for (var trackedEntity : this.entityMap.values()) {
            var entity = ((ChunkMap_TrackedEntityBridge) trackedEntity).bridge$getEntity();
            if (entity instanceof ServerPlayer player && ((ServerPlayerEntityBridge) player).bridge$isTrackerDirty()) {
                list.add(trackedEntity);
                ((ServerPlayerEntityBridge) player).bridge$setTrackerDirty(false);
            }
            ((ChunkMap_TrackedEntityBridge) trackedEntity).bridge$getServerEntity().sendChanges();
        }

        for (var trackedEntity : this.entityMap.values()) {
            var entity = ((ChunkMap_TrackedEntityBridge) trackedEntity).bridge$getEntity();
            SectionPos lastSectionPos = ((ChunkMap_TrackedEntityBridge) trackedEntity).bridge$getLastSectionPos();
            SectionPos newSectionPos = SectionPos.of(entity);
            ((ChunkMap_TrackedEntityBridge) trackedEntity).bridge$setLastSectionPos(newSectionPos);
            if (entity instanceof ServerPlayer player) {
                for (var otherTracker : list) {
                    var other = (ServerPlayer) ((ChunkMap_TrackedEntityBridge) otherTracker).bridge$getEntity();
                    if (other.getId() > entity.getId()) {
                        trackedEntity.updatePlayer(other);
                        otherTracker.updatePlayer(player);
                    }
                }
            } else {
                boolean chunkChanged = !Objects.equals(lastSectionPos, newSectionPos);
                if (chunkChanged) {
                    trackedEntity.updatePlayers(this.level.players());
                } else {
                    for (var other : list) {
                        trackedEntity.updatePlayer((ServerPlayer) ((ChunkMap_TrackedEntityBridge) other).bridge$getEntity());
                    }
                }
            }
        }
        ci.cancel();
    }

    @Mixin(ChunkMap.TrackedEntity.class)
    public static class TrackedEntityMixin {

        @Redirect(method = "<init>", at = @At(value = "INVOKE", remap = false, target = "Lcom/google/common/collect/Sets;newIdentityHashSet()Ljava/util/Set;"))
        private Set<ServerPlayerConnection> arclight$useFastUtilSet() {
            return new ReferenceOpenHashSet<>();
        }
    }
}
