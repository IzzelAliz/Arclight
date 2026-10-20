package io.izzel.arclight.common.mixin.core.server.level;

import io.izzel.arclight.common.bridge.core.server.level.ServerPlayerBridge;
import io.izzel.arclight.common.mod.compat.ModIds;
import io.izzel.arclight.common.mod.mixins.annotation.LoadIfMod;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// https://github.com/IzzelAliz/Arclight/issues/2145
// ImmPtl replaces vanilla tracking; omit Bukkit canSee here rather than inject into its empty method.
@Mixin(ChunkMap.TrackedEntity.class)
@LoadIfMod(modid = ModIds.IMMERSIVE_PORTALS, condition = LoadIfMod.ModCondition.ABSENT)
public abstract class ChunkMap_TrackedEntityMixin_Vanishing {

    @Decorate(method = "updatePlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;broadcastToPlayer(Lnet/minecraft/server/level/ServerPlayer;)Z"))
    private boolean arclight$implementVanishing(Entity instance, ServerPlayer serverPlayer) throws Throwable {
        boolean canSee =  ((ServerPlayerBridge)serverPlayer).bridge$getBukkitEntity().canSee(instance.bridge$getBukkitEntity());
        boolean canBroadcast = (boolean) DecorationOps.callsite().invoke(instance, serverPlayer);
        return canSee && canBroadcast;
    }

}
