package io.izzel.arclight.common.mixin.core.world.level.gameevent.vibrations;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.PositionSource;
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
    @Shadow @Nullable protected VibrationListener.ReceivingEvent receivingEvent;
    @Shadow @Final protected VibrationListener.VibrationListenerConfig config;
    @Shadow @Final protected PositionSource listenerSource;
    @Shadow private static boolean isOccluded(Level p_223776_, Vec3 p_223777_, Vec3 p_223778_) { return false; }
    @Shadow protected abstract void scheduleSignal(ServerLevel p_223770_, GameEvent p_223771_, GameEvent.Context p_223772_, Vec3 p_223773_, Vec3 p_223774_);
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public boolean handleGameEvent(ServerLevel level, GameEvent.Message p_223768_) {
        if (this.receivingEvent != null) {
            return false;
        } else {
            GameEvent gameevent = p_223768_.gameEvent();
            GameEvent.Context gameevent$context = p_223768_.context();
            if (!this.config.isValidVibration(gameevent, gameevent$context)) {
                return false;
            } else {
                Optional<Vec3> optional = this.listenerSource.getPosition(level);
                if (optional.isEmpty()) {
                    return false;
                } else {
                    Vec3 vec3 = p_223768_.source();
                    Vec3 vec31 = optional.get();
                    boolean cancelled = !this.config.shouldListen(level, (VibrationListener) (Object) this, new BlockPos(vec3), gameevent, gameevent$context);
                    Entity entity = gameevent$context.sourceEntity();
                    var event = new BlockReceiveGameEvent(org.bukkit.GameEvent.getByKey(CraftNamespacedKey.fromMinecraft(Registry.GAME_EVENT.getKey(gameevent))), CraftBlock.at(level, new BlockPos(vec31)), (entity == null) ? null : ((EntityBridge) entity).bridge$getBukkitEntity());
                    event.setCancelled(cancelled);
                    Bukkit.getPluginManager().callEvent(event);
                    if (event.isCancelled()) {
                        return false;
                    } else if (isOccluded(level, vec3, vec31)) {
                        return false;
                    } else {
                        this.scheduleSignal(level, gameevent, gameevent$context, vec3, vec31);
                        return true;
                    }
                }
            }
        }
    }
}
