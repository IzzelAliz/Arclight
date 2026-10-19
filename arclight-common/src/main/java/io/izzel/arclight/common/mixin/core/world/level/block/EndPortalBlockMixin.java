package io.izzel.arclight.common.mixin.core.world.level.block;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReceiver;
import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.server.level.ServerPlayerBridge;
import io.izzel.arclight.common.bridge.core.world.level.WorldBridge;
import io.izzel.arclight.common.bridge.core.world.level.portal.DimensionTransitionBridge;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EndPortalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.portal.TeleportTransition;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.util.CraftLocation;
import org.bukkit.event.entity.EntityPortalEnterEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EndPortalBlock.class)
public class EndPortalBlockMixin {

    @ModifyExpressionValue(method = "entityInside", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;canUsePortal(Z)Z"))
    private boolean arclight$enterPortal(boolean allowed, BlockState blockState, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isInside) {
        if (allowed) {
            EntityPortalEnterEvent event = new EntityPortalEnterEvent(entity.bridge$getBukkitEntity(),
                new Location(level.bridge$getWorld(), pos.getX(), pos.getY(), pos.getZ()));
            Bukkit.getPluginManager().callEvent(event);
        }
        return allowed;
    }

    @ModifyReceiver(method = "getPortalDestination", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;getRespawnData()Lnet/minecraft/world/level/storage/LevelData$RespawnData;"))
    private ServerLevel arclight$mainRespawn(ServerLevel level) {
        return level.getServer().findRespawnDimension();
    }

    @ModifyVariable(method = "getPortalDestination", at = @At("STORE"), ordinal = 0)
    private boolean arclight$fromEndType(boolean original, ServerLevel level, Entity entity, BlockPos pos) {
        return ((WorldBridge) level).bridge$getTypeKey() == LevelStem.END;
    }

    @Inject(method = "getPortalDestination", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;findRespawnPositionAndUseSpawnBlock(ZLnet/minecraft/world/level/portal/TeleportTransition$PostTeleportTransition;)Lnet/minecraft/world/level/portal/TeleportTransition;"))
    private void arclight$pushCause(ServerLevel serverLevel, Entity entity, BlockPos blockPos, CallbackInfoReturnable<TeleportTransition> cir) {
        ((ServerPlayerBridge) entity).bridge$pushRespawnReason(PlayerRespawnEvent.RespawnReason.END_PORTAL);
    }

    @Inject(method = "getPortalDestination", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/feature/EndPlatformFeature;createEndPlatform(Lnet/minecraft/world/level/ServerLevelAccessor;Lnet/minecraft/core/BlockPos;Z)V"))
    private void arclight$pushEntity(ServerLevel serverLevel, Entity entity, BlockPos blockPos, CallbackInfoReturnable<TeleportTransition> cir) {
        ArclightCaptures.captureEndPortalEntity(entity, true);
    }

    @Inject(method = "getPortalDestination", at = @At("RETURN"), cancellable = true,
        slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/world/level/portal/TeleportTransition;PLAY_PORTAL_SOUND:Lnet/minecraft/world/level/portal/TeleportTransition$PostTeleportTransition;")))
    private void arclight$fireEvent(ServerLevel serverLevel, Entity entity, BlockPos blockPos, CallbackInfoReturnable<TeleportTransition> cir) {
        var dt = cir.getReturnValue();
        var event = ((EntityBridge) entity).bridge$callPortalEvent(entity, CraftLocation.toBukkit(dt.position(), dt.newLevel().bridge$getWorld(), dt.yRot(), dt.xRot()), PlayerTeleportEvent.TeleportCause.END_PORTAL, 0, 0);
        if (event == null) {
            cir.setReturnValue(null);
            return;
        }
        Location to = event.getTo();
        var newDt = new TeleportTransition(((CraftWorld) to.getWorld()).getHandle(), CraftLocation.toVec3D(to), dt.deltaMovement(), to.getYaw(), to.getPitch(), dt.missingRespawnBlock(), dt.asPassenger(), dt.relatives(), dt.postTeleportTransition());
        ((DimensionTransitionBridge) (Object) newDt).bridge$setTeleportCause(PlayerTeleportEvent.TeleportCause.END_PORTAL);
        cir.setReturnValue(newDt);
    }
}
