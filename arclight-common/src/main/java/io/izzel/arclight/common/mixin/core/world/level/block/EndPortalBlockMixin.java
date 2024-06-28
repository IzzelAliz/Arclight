package io.izzel.arclight.common.mixin.core.world.level.block;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.core.world.level.portal.DimensionTransitionBridge;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EndPortalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.DimensionTransition;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v.CraftWorld;
import org.bukkit.craftbukkit.v.util.CraftLocation;
import org.bukkit.event.entity.EntityPortalEnterEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EndPortalBlock.class)
public class EndPortalBlockMixin {

    @Inject(method = "entityInside", at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/Level;isClientSide:Z"))
    public void arclight$enterPortal(BlockState blockState, Level level, BlockPos pos, Entity entity, CallbackInfo ci) {
        EntityPortalEnterEvent event = new EntityPortalEnterEvent(entity.bridge$getBukkitEntity(),
            new Location(level.bridge$getWorld(), pos.getX(), pos.getY(), pos.getZ()));
        Bukkit.getPluginManager().callEvent(event);
    }

    @Inject(method = "getPortalDestination", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;findRespawnPositionAndUseSpawnBlock(ZLnet/minecraft/world/level/portal/DimensionTransition$PostDimensionTransition;)Lnet/minecraft/world/level/portal/DimensionTransition;"))
    private void arclight$pushCause(ServerLevel serverLevel, Entity entity, BlockPos blockPos, CallbackInfoReturnable<DimensionTransition> cir) {
        ((ServerPlayerEntityBridge) entity).bridge$pushRespawnReason(PlayerRespawnEvent.RespawnReason.END_PORTAL);
    }

    @Inject(method = "getPortalDestination", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/feature/EndPlatformFeature;createEndPlatform(Lnet/minecraft/world/level/ServerLevelAccessor;Lnet/minecraft/core/BlockPos;Z)V"))
    private void arclight$pushEntity(ServerLevel serverLevel, Entity entity, BlockPos blockPos, CallbackInfoReturnable<DimensionTransition> cir) {
        ArclightCaptures.captureEndPortalEntity(entity, true);
    }

    @Inject(method = "getPortalDestination", at = @At("RETURN"), cancellable = true,
        slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/world/level/portal/DimensionTransition;PLAY_PORTAL_SOUND:Lnet/minecraft/world/level/portal/DimensionTransition$PostDimensionTransition;")))
    private void arclight$fireEvent(ServerLevel serverLevel, Entity entity, BlockPos blockPos, CallbackInfoReturnable<DimensionTransition> cir) {
        var dt = cir.getReturnValue();
        var event = ((EntityBridge) entity).bridge$callPortalEvent(entity, CraftLocation.toBukkit(dt.pos(), dt.newLevel().bridge$getWorld(), dt.yRot(), dt.xRot()), PlayerTeleportEvent.TeleportCause.END_PORTAL, 0, 0);
        if (event == null) {
            cir.setReturnValue(null);
            return;
        }
        Location to = event.getTo();
        var newDt = new DimensionTransition(((CraftWorld) to.getWorld()).getHandle(), CraftLocation.toVec3D(to), entity.getDeltaMovement(), to.getYaw(), to.getPitch(), DimensionTransition.PLAY_PORTAL_SOUND.then(DimensionTransition.PLACE_PORTAL_TICKET));
        ((DimensionTransitionBridge) (Object) newDt).bridge$setTeleportCause(PlayerTeleportEvent.TeleportCause.END_PORTAL);
        cir.setReturnValue(newDt);
    }
}
