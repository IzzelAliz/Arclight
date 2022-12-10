package io.izzel.arclight.common.mixin.core.world.level.gameevent.vibrations;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.level.gameevent.vibrations.VibrationInfo;
import net.minecraft.world.level.gameevent.vibrations.VibrationListener;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.util.CraftNamespacedKey;
import org.bukkit.event.block.BlockReceiveGameEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;
import java.util.Optional;

@Mixin(VibrationListener.class)
public abstract class VibrationListenerMixin {

    // @formatter:off
    @Shadow @Final protected VibrationListener.VibrationListenerConfig config;
    @Shadow @Final protected PositionSource listenerSource;
    @Shadow private static boolean isOccluded(Level p_223776_, Vec3 p_223777_, Vec3 p_223778_) { return false; }
    @Shadow @Nullable protected VibrationInfo currentVibration;
    @Shadow public abstract void scheduleVibration(ServerLevel p_250210_, GameEvent p_251063_, GameEvent.Context p_249354_, Vec3 p_250310_, Vec3 p_249553_);
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public boolean handleGameEvent(ServerLevel worldserver, GameEvent gameevent, GameEvent.Context gameevent_a, Vec3 vec3d) {
        if (this.currentVibration != null) {
            return false;
        } else if (!this.config.isValidVibration(gameevent, gameevent_a)) {
            return false;
        } else {
            Optional<Vec3> optional = this.listenerSource.getPosition(worldserver);

            if (optional.isEmpty()) {
                return false;
            } else {
                Vec3 vec3d1 = optional.get();

                // CraftBukkit start
                boolean defaultCancel = !this.config.shouldListen(worldserver, (VibrationListener) (Object) this, new BlockPos(vec3d), gameevent, gameevent_a);
                Entity entity = gameevent_a.sourceEntity();
                BlockReceiveGameEvent event = new BlockReceiveGameEvent(org.bukkit.GameEvent.getByKey(CraftNamespacedKey.fromMinecraft(BuiltInRegistries.GAME_EVENT.getKey(gameevent))), CraftBlock.at(worldserver, new BlockPos(vec3d1)), (entity == null) ? null : ((EntityBridge) entity).bridge$getBukkitEntity());
                event.setCancelled(defaultCancel);
                Bukkit.getPluginManager().callEvent(event);
                if (event.isCancelled()) {
                    // CraftBukkit end
                    return false;
                } else if (isOccluded(worldserver, vec3d, vec3d1)) {
                    return false;
                } else {
                    this.scheduleVibration(worldserver, gameevent, gameevent_a, vec3d, vec3d1);
                    return true;
                }
            }
        }
    }
}
