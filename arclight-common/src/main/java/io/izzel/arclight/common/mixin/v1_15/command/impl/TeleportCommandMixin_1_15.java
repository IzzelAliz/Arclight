package io.izzel.arclight.common.mixin.v1_15.command.impl;

import io.izzel.arclight.common.bridge.entity.EntityBridge;
import io.izzel.arclight.common.bridge.network.play.ServerPlayNetHandlerBridge;
import io.izzel.arclight.common.bridge.world.server.ServerWorldBridge;
import net.minecraft.command.CommandSource;
import net.minecraft.command.impl.TeleportCommand;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.server.SPlayerPositionLookPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.server.TicketType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v.CraftWorld;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import javax.annotation.Nullable;
import java.util.Set;

@Mixin(TeleportCommand.class)
public class TeleportCommandMixin_1_15 {

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    private static void teleport(CommandSource source, Entity entityIn, ServerWorld worldIn, double x, double y, double z, Set<SPlayerPositionLookPacket.Flags> relativeList, float yaw, float pitch, @Nullable TeleportCommand.Facing facing) {
        if (entityIn instanceof ServerPlayerEntity) {
            ChunkPos chunkpos = new ChunkPos(new BlockPos(x, y, z));
            worldIn.getChunkProvider().registerTicket(TicketType.POST_TELEPORT, chunkpos, 1, entityIn.getEntityId());
            entityIn.stopRiding();
            if (((ServerPlayerEntity) entityIn).isSleeping()) {
                ((ServerPlayerEntity) entityIn).stopSleepInBed(true, true);
            }

            ((ServerPlayNetHandlerBridge) ((ServerPlayerEntity) entityIn).connection).bridge$pushTeleportCause(PlayerTeleportEvent.TeleportCause.COMMAND);
            if (worldIn == entityIn.world) {
                ((ServerPlayerEntity) entityIn).connection.setPlayerLocation(x, y, z, yaw, pitch, relativeList);
            } else {
                ((ServerPlayerEntity) entityIn).teleport(worldIn, x, y, z, yaw, pitch);
            }

            entityIn.setRotationYawHead(yaw);
        } else {
            float f1 = MathHelper.wrapDegrees(yaw);
            float f = MathHelper.wrapDegrees(pitch);
            f = MathHelper.clamp(f, -90.0F, 90.0F);

            Location to = new Location(((ServerWorldBridge) worldIn).bridge$getWorld(), x, y, z, f1, f);
            EntityTeleportEvent event = new EntityTeleportEvent(((EntityBridge) entityIn).bridge$getBukkitEntity(), ((EntityBridge) entityIn).bridge$getBukkitEntity().getLocation(), to);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return;
            }
            to = event.getTo();
            x = to.getX();
            y = to.getY();
            z = to.getZ();
            f1 = to.getYaw();
            f = to.getPitch();
            worldIn = ((CraftWorld) to.getWorld()).getHandle();

            if (worldIn == entityIn.world) {
                entityIn.setLocationAndAngles(x, y, z, f1, f);
                entityIn.setRotationYawHead(f1);
            } else {
                entityIn.detach();
                entityIn.dimension = worldIn.dimension.getType();
                Entity entity = entityIn;
                entityIn = entityIn.getType().create(worldIn);
                if (entityIn == null) {
                    return;
                }

                entityIn.copyDataFromOld(entity);
                entityIn.setLocationAndAngles(x, y, z, f1, f);
                entityIn.setRotationYawHead(f1);
                worldIn.addFromAnotherDimension(entityIn);
                entity.removed = true;
            }
        }

        if (facing != null) {
            facing.updateLook(source, entityIn);
        }

        if (!(entityIn instanceof LivingEntity) || !((LivingEntity) entityIn).isElytraFlying()) {
            entityIn.setMotion(entityIn.getMotion().mul(1.0D, 0.0D, 1.0D));
            entityIn.onGround = true;
        }

    }
}
