package io.izzel.arclight.common.mixin.core.tileentity;

import io.izzel.arclight.common.bridge.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.network.play.ServerPlayNetHandlerBridge;
import io.izzel.arclight.common.bridge.world.WorldBridge;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.tileentity.EndGatewayTileEntity;
import net.minecraft.util.math.BlockPos;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v.entity.CraftPlayer;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(EndGatewayTileEntity.class)
public abstract class EndGatewayTileEntityMixin extends TileEntityMixin {

    // @formatter:off
    @Shadow public abstract void triggerCooldown();
    // @formatter:on

    @Inject(method = "teleportEntity", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;teleportKeepLoaded(DDD)V"))
    public void arclight$portal(Entity entityIn, CallbackInfo ci, BlockPos pos) {
        if (entityIn instanceof ServerPlayerEntity) {
            CraftPlayer player = ((ServerPlayerEntityBridge) entityIn).bridge$getBukkitEntity();
            Location location = new Location(((WorldBridge) world).bridge$getWorld(), pos.getX() + 0.5D, pos.getY() + 0.5D,  pos.getZ() + 0.5D);
            location.setPitch(player.getLocation().getPitch());
            location.setYaw(player.getLocation().getYaw());

            PlayerTeleportEvent event = new PlayerTeleportEvent(player, player.getLocation(), location, PlayerTeleportEvent.TeleportCause.END_GATEWAY);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                ci.cancel();
                return;
            }

            ((ServerPlayNetHandlerBridge) (((ServerPlayerEntity) entityIn)).connection).bridge$teleport(event.getTo());
            this.triggerCooldown(); // CraftBukkit - call at end of method
            ci.cancel();
        }
    }
}
