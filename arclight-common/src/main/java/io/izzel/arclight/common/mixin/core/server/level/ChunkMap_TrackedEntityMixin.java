package io.izzel.arclight.common.mixin.core.server.level;

import io.izzel.arclight.common.bridge.core.server.level.ServerPlayerBridge;
import io.izzel.arclight.common.bridge.core.server.level.ServerEntityBridge;
import io.izzel.arclight.common.bridge.core.world.server.ChunkMap_TrackedEntityBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
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

    @Decorate(method = "updatePlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;broadcastToPlayer(Lnet/minecraft/server/level/ServerPlayer;)Z"))
    private boolean arclight$implementVanishing(Entity instance, ServerPlayer serverPlayer) throws Throwable {
        boolean canSee =  ((ServerPlayerBridge)serverPlayer).bridge$getBukkitEntity().canSee(instance.bridge$getBukkitEntity());
        boolean canBroadcast = (boolean) DecorationOps.callsite().invoke(instance, serverPlayer);
        return canSee && canBroadcast;
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
