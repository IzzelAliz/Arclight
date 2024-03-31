package io.izzel.arclight.common.mixin.core.world.level.block.entity;

import io.izzel.arclight.api.ArclightPlatform;
import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.core.network.play.ServerPlayNetHandlerBridge;
import io.izzel.arclight.common.bridge.core.tileentity.EndGatewayBlockEntityBridge;
import io.izzel.arclight.common.mod.mixins.annotation.OnlyInPlatform;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.TheEndGatewayBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v.entity.CraftPlayer;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(TheEndGatewayBlockEntity.class)
public abstract class EndGatewayBlockEntityMixin extends BlockEntityMixin implements EndGatewayBlockEntityBridge {

    // @formatter:off
    @Shadow private static void triggerCooldown(Level p_155850_, BlockPos p_155851_, BlockState p_155852_, TheEndGatewayBlockEntity p_155853_) { }
    // @formatter:on

    @Inject(method = "teleportEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;setPortalCooldown()V"))
    private static void arclight$teleportCause(Level level, BlockPos pos, BlockState state, Entity entityIn, TheEndGatewayBlockEntity entity, CallbackInfo ci) {
        entityIn.bridge().bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.HIT);
    }

    @Override
    public void bridge$playerTeleportEvent(Level level, BlockPos pos, BlockState state, Entity entityIn, BlockPos dest) {
        CraftPlayer player = ((ServerPlayerEntityBridge) entityIn).bridge$getBukkitEntity();
        Location location = new Location(level.bridge$getWorld(), dest.getX() + 0.5D, dest.getY() + 0.5D, dest.getZ() + 0.5D);
        location.setPitch(player.getLocation().getPitch());
        location.setYaw(player.getLocation().getYaw());

        PlayerTeleportEvent event = new PlayerTeleportEvent(player, player.getLocation(), location, PlayerTeleportEvent.TeleportCause.END_GATEWAY);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }

        entityIn.setPortalCooldown();
        ((ServerPlayNetHandlerBridge) (((ServerPlayer) entityIn)).connection).bridge$teleport(event.getTo());
        triggerCooldown(level, pos, state, (TheEndGatewayBlockEntity) (Object) this);
    }

    @Mixin(TheEndGatewayBlockEntity.class)
    @OnlyInPlatform({ArclightPlatform.FABRIC, ArclightPlatform.VANILLA})
    public static class VanillaLike {

        @Inject(method = "teleportEntity", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;setPortalCooldown()V"))
        private static void arclight$portal(Level level, BlockPos pos, BlockState state, Entity entityIn, TheEndGatewayBlockEntity entity, CallbackInfo ci,
                                            BlockPos dest) {
            if (entityIn instanceof ServerPlayer) {
                ((EndGatewayBlockEntityBridge) entity).bridge$playerTeleportEvent(level, pos, state, entityIn, dest);
                ci.cancel();
            }
        }
    }

    @Mixin(TheEndGatewayBlockEntity.class)
    @OnlyInPlatform({ArclightPlatform.FORGE, ArclightPlatform.NEOFORGE})
    public static class ForgeLike {

        @Inject(method = "teleportEntity", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;setPortalCooldown()V"))
        private static void arclight$portal(Level level, BlockPos pos, BlockState state, Entity entityIn, TheEndGatewayBlockEntity entity, CallbackInfo ci,
                                            ServerLevel serverLevel, BlockPos dest) {
            if (entityIn instanceof ServerPlayer) {
                ((EndGatewayBlockEntityBridge) entity).bridge$playerTeleportEvent(level, pos, state, entityIn, dest);
                ci.cancel();
            }
        }
    }
}
