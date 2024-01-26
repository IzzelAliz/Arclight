package io.izzel.arclight.common.mixin.core.server.commands;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.core.world.server.ServerWorldBridge;
import io.izzel.tools.product.Product;
import io.izzel.tools.product.Product4;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.commands.TeleportCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.level.Level;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v.CraftWorld;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.spongepowered.asm.mixin.*;

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
    private static void performTeleport(CommandSourceStack source, Entity entity, ServerLevel level, double x, double y, double z, Set<RelativeMovement> set, float yaw, float pitch, @Nullable TeleportCommand.LookAt p_139024_) throws CommandSyntaxException {
        var event = ((EntityBridge) entity).bridge$onEntityTeleportCommand(x, y, z);
        if (event._1) {
            return;
        }
        x = event._2;
        y = event._3;
        z = event._4;
        BlockPos blockpos = BlockPos.containing(x, y, z);
        if (!Level.isInSpawnableBounds(blockpos)) {
            throw INVALID_POSITION.create();
        } else {
            float f = Mth.wrapDegrees(yaw);
            float f1 = Mth.wrapDegrees(pitch);

            boolean result;
            if (entity instanceof ServerPlayer player) {
                ((ServerPlayerEntityBridge) player).bridge$pushChangeDimensionCause(PlayerTeleportEvent.TeleportCause.COMMAND);
                result = player.teleportTo(level, x, y, z, set, f, f1);
            } else {
                Location to = new Location(((ServerWorldBridge) level).bridge$getWorld(), x, y, z, yaw, pitch);
                var e = new org.bukkit.event.entity.EntityTeleportEvent(((EntityBridge) entity).bridge$getBukkitEntity(), ((EntityBridge) entity).bridge$getBukkitEntity().getLocation(), to);
                Bukkit.getPluginManager().callEvent(e);
                if (e.isCancelled()) {
                    return;
                }

                x = to.getX();
                y = to.getY();
                z = to.getZ();
                f = to.getYaw();
                f1 = to.getPitch();
                level = ((CraftWorld) to.getWorld()).getHandle();

                result = entity.teleportTo(level, x, y, z, set, f, f1);
            }

            if (result) {
                if (p_139024_ != null) {
                    p_139024_.perform(source, entity);
                }

                label23:
                {
                    if (entity instanceof LivingEntity) {
                        LivingEntity livingentity = (LivingEntity) entity;
                        if (livingentity.isFallFlying()) {
                            break label23;
                        }
                    }

                    entity.setDeltaMovement(entity.getDeltaMovement().multiply(1.0D, 0.0D, 1.0D));
                    entity.setOnGround(true);
                }

                if (entity instanceof PathfinderMob) {
                    PathfinderMob pathfindermob = (PathfinderMob) entity;
                    pathfindermob.getNavigation().stop();
                }

            }
        }
    }
}
