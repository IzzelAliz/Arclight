package io.izzel.arclight.common.mixin.core.world.item;

import io.izzel.arclight.common.bridge.core.world.level.WorldBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.EnderEyeItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EnderEyeItem.class)
public class EnderEyeItemMixin {

    @Decorate(method = "useOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;globalLevelEvent(ILnet/minecraft/core/BlockPos;I)V"))
    private void arclight$portalSound(Level level, int event, BlockPos soundPos, int data) throws Throwable {
        if (event != 1038) {
            DecorationOps.callsite().invoke(level, event, soundPos, data);
            return;
        }
        int viewDistance = ((WorldBridge) level).bridge$getServer().getViewDistance() * 16;
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            double deltaX = soundPos.getX() - player.getX();
            double deltaZ = soundPos.getZ() - player.getZ();
            double distanceSquared = deltaX * deltaX + deltaZ * deltaZ;
            if (((WorldBridge) level).bridge$spigotConfig().endPortalSoundRadius > 0
                && distanceSquared > ((WorldBridge) level).bridge$spigotConfig().endPortalSoundRadius
                * ((WorldBridge) level).bridge$spigotConfig().endPortalSoundRadius) continue;
            if (distanceSquared > viewDistance * viewDistance) {
                double deltaLength = Math.sqrt(distanceSquared);
                double relativeX = player.getX() + (deltaX / deltaLength) * viewDistance;
                double relativeZ = player.getZ() + (deltaZ / deltaLength) * viewDistance;
                player.connection.send(new ClientboundLevelEventPacket(event, new BlockPos((int) relativeX, soundPos.getY(), (int) relativeZ), data, true));
            } else {
                player.connection.send(new ClientboundLevelEventPacket(event, soundPos, data, true));
            }
        }
    }

    @Decorate(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean arclight$returnIfFail(Level world, Entity entityIn, Level worldIn, Player playerIn, InteractionHand handIn) throws Throwable {
        if (!(boolean) DecorationOps.callsite().invoke(world, entityIn)) {
            return (boolean) DecorationOps.cancel().invoke((InteractionResult) InteractionResult.FAIL);
        } else {
            return true;
        }
    }
}
