package io.izzel.arclight.common.mixin.core.world.level.gameevent.vibrations;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.CraftGameEvent;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.event.block.BlockReceiveGameEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Optional;

@Mixin(VibrationSystem.Listener.class)
public abstract class VibrationListenerMixin {

    // @formatter:off
    @Shadow private static boolean isOccluded(Level p_223776_, Vec3 p_223777_, Vec3 p_223778_) { return false; }
    @Shadow @Final private VibrationSystem system;
    @Shadow protected abstract void scheduleVibration(ServerLevel p_282037_, VibrationSystem.Data p_283229_, GameEvent p_281778_, GameEvent.Context p_283344_, Vec3 p_281758_, Vec3 p_282990_);
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public boolean handleGameEvent(ServerLevel worldserver, GameEvent gameevent, GameEvent.Context gameevent_a, Vec3 vec3d) {
        VibrationSystem.Data vibrationsystem_a = this.system.getVibrationData();
        VibrationSystem.User vibrationsystem_d = this.system.getVibrationUser();

        if (vibrationsystem_a.getCurrentVibration() != null) {
            return false;
        } else if (!vibrationsystem_d.isValidVibration(gameevent, gameevent_a)) {
            return false;
        } else {
            Optional<Vec3> optional = vibrationsystem_d.getPositionSource().getPosition(worldserver);

            if (optional.isEmpty()) {
                return false;
            } else {
                Vec3 vec3d1 = optional.get();
                // CraftBukkit start
                boolean defaultCancel = !vibrationsystem_d.canReceiveVibration(worldserver, BlockPos.containing(vec3d), gameevent, gameevent_a);
                Entity entity = gameevent_a.sourceEntity();
                BlockReceiveGameEvent event = new BlockReceiveGameEvent(CraftGameEvent.minecraftToBukkit(gameevent), CraftBlock.at(worldserver, BlockPos.containing(vec3d1)), (entity == null) ? null : ((EntityBridge) entity).bridge$getBukkitEntity());
                event.setCancelled(defaultCancel);
                Bukkit.getPluginManager().callEvent(event);
                if (event.isCancelled()) {
                    // CraftBukkit end
                    return false;
                } else if (isOccluded(worldserver, vec3d, vec3d1)) {
                    return false;
                } else {
                    this.scheduleVibration(worldserver, vibrationsystem_a, gameevent, gameevent_a, vec3d, vec3d1);
                    return true;
                }
            }
        }
    }
}
