package io.izzel.arclight.common.mixin.core.server.level;

import io.izzel.arclight.common.bridge.core.world.ServerEntityBridge;
import io.izzel.arclight.common.bridge.core.world.server.ChunkMap_TrackedEntityBridge;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(ChunkMap.TrackedEntity.class)
public abstract class ChunkMap_TrackedEntityMixin implements ChunkMap_TrackedEntityBridge {

    // @formatter:off
    @Shadow @Final ServerEntity serverEntity;
    @Shadow @Final public Set<ServerPlayerConnection> seenBy;
    @Shadow @Final Entity entity;
    @Shadow SectionPos lastSectionPos;
    // @formatter:on

    @Inject(method = "<init>", at = @At("RETURN"))
    private void arclight$setTrackedPlayers(ChunkMap outer, Entity entity, int range, int updateFrequency, boolean sendVelocityUpdates, CallbackInfo ci) {
        ((ServerEntityBridge) this.serverEntity).bridge$setTrackedPlayers(this.seenBy);
    }

    @Override
    public ServerEntity bridge$getServerEntity() {
        return this.serverEntity;
    }

    @Override
    public Entity bridge$getEntity() {
        return this.entity;
    }

    @Override
    public SectionPos bridge$getLastSectionPos() {
        return this.lastSectionPos;
    }

    @Override
    public void bridge$setLastSectionPos(SectionPos pos) {
        this.lastSectionPos = pos;
    }
}
