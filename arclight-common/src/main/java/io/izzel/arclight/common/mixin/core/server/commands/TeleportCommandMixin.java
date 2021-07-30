package io.izzel.arclight.common.mixin.core.server.commands;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.core.network.play.ServerPlayNetHandlerBridge;
import io.izzel.arclight.common.bridge.core.world.server.ServerWorldBridge;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.server.commands.TeleportCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v.CraftWorld;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;
import java.util.Set;

@Mixin(TeleportCommand.class)
public class TeleportCommandMixin {

    // @formatter:off
    @Shadow @Final private static SimpleCommandExceptionType INVALID_POSITION;
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    private static void performTeleport(CommandSourceStack source, Entity teleport, ServerLevel level, double x, double y, double z, Set<ClientboundPlayerPositionPacket.RelativeArgument> p_139021_, float p_139022_, float p_139023_, @Nullable TeleportCommand.LookAt p_139024_) throws CommandSyntaxException {
        EntityTeleportEvent.TeleportCommand event = ForgeEventFactory.onEntityTeleportCommand(teleport, x, y, z);
        if (event.isCanceled()) return;
        x = event.getTargetX();
        y = event.getTargetY();
        z = event.getTargetZ();
        BlockPos blockpos = new BlockPos(x, y, z);
        if (!Level.isInSpawnableBounds(blockpos)) {
            throw INVALID_POSITION.create();
        } else {
            float yaw = Mth.wrapDegrees(p_139022_);
            float f1 = Mth.wrapDegrees(p_139023_);
            if (teleport instanceof ServerPlayer) {
                ChunkPos chunkpos = new ChunkPos(new BlockPos(x, y, z));
                level.getChunkSource().addRegionTicket(TicketType.POST_TELEPORT, chunkpos, 1, teleport.getId());
                teleport.stopRiding();
                if (((ServerPlayer) teleport).isSleeping()) {
                    ((ServerPlayer) teleport).stopSleepInBed(true, true);
                }

                if (level == teleport.level) {
                    ((ServerPlayNetHandlerBridge) ((ServerPlayer) teleport).connection).bridge$pushTeleportCause(PlayerTeleportEvent.TeleportCause.COMMAND);
                    ((ServerPlayer) teleport).connection.teleport(x, y, z, yaw, f1, p_139021_);
                } else {
                    ((ServerPlayerEntityBridge) teleport).bridge$pushChangeDimensionCause(PlayerTeleportEvent.TeleportCause.COMMAND);
                    ((ServerPlayer) teleport).teleportTo(level, x, y, z, yaw, f1);
                }

                teleport.setYHeadRot(yaw);
            } else {
                float pitch = Mth.clamp(f1, -90.0F, 90.0F);
                Location to = new Location(((ServerWorldBridge) level).bridge$getWorld(), x, y, z, pitch, yaw);
                org.bukkit.event.entity.EntityTeleportEvent bukkitEvent = new org.bukkit.event.entity.EntityTeleportEvent(((EntityBridge) teleport).bridge$getBukkitEntity(), ((EntityBridge) teleport).bridge$getBukkitEntity().getLocation(), to);
                Bukkit.getPluginManager().callEvent(bukkitEvent);
                if (bukkitEvent.isCancelled()) {
                    return;
                }

                x = to.getX();
                y = to.getY();
                z = to.getZ();
                yaw = to.getYaw();
                pitch = to.getPitch();
                level = ((CraftWorld) to.getWorld()).getHandle();
                if (level == teleport.level) {
                    teleport.moveTo(x, y, z, yaw, pitch);
                    teleport.setYHeadRot(yaw);
                } else {
                    teleport.unRide();
                    Entity entity = teleport;
                    teleport = teleport.getType().create(level);
                    if (teleport == null) {
                        return;
                    }

                    teleport.restoreFrom(entity);
                    teleport.moveTo(x, y, z, yaw, pitch);
                    teleport.setYHeadRot(yaw);
                    entity.setRemoved(Entity.RemovalReason.CHANGED_DIMENSION);
                    level.addDuringTeleport(teleport);
                }
            }

            if (p_139024_ != null) {
                p_139024_.perform(source, teleport);
            }

            if (!(teleport instanceof LivingEntity) || !((LivingEntity) teleport).isFallFlying()) {
                teleport.setDeltaMovement(teleport.getDeltaMovement().multiply(1.0D, 0.0D, 1.0D));
                teleport.setOnGround(true);
            }

            if (teleport instanceof PathfinderMob) {
                ((PathfinderMob) teleport).getNavigation().stop();
            }

        }
    }
}
