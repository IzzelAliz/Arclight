package io.izzel.arclight.common.mixin.core.world.level.gameevent.vibrations;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.level.gameevent.vibrations.VibrationListener;
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
    @Shadow protected abstract boolean isValidVibration(GameEvent p_157917_, @org.jetbrains.annotations.Nullable Entity p_157918_);
    @Shadow @Final protected PositionSource listenerSource;
    @Shadow @Final protected VibrationListener.VibrationListenerConfig config;
    @Shadow protected abstract boolean isOccluded(Level p_157911_, BlockPos p_157912_, BlockPos p_157913_);
    @Shadow protected abstract void sendSignal(Level p_157906_, GameEvent p_157907_, BlockPos p_157908_, BlockPos p_157909_);
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public boolean handleGameEvent(Level level, GameEvent gameEvent, @Nullable Entity entity, BlockPos pos) {
        if (!this.isValidVibration(gameEvent, entity)) {
            return false;
        } else {
            Optional<BlockPos> optional = this.listenerSource.getPosition(level);
            if (!optional.isPresent()) {
                return false;
            } else {
                BlockPos blockpos = optional.get();
                var cancelled = !this.config.shouldListen(level, (VibrationListener) (Object) this, pos, gameEvent, entity);
                BlockReceiveGameEvent event = new BlockReceiveGameEvent(org.bukkit.GameEvent.getByKey(CraftNamespacedKey.fromMinecraft(Registry.GAME_EVENT.getKey(gameEvent))),
                    CraftBlock.at(level, blockpos), (entity == null) ? null : ((EntityBridge) entity).bridge$getBukkitEntity());
                event.setCancelled(cancelled);
                Bukkit.getPluginManager().callEvent(event);
                if (cancelled) {
                    return false;
                } else if (this.isOccluded(level, pos, blockpos)) {
                    return false;
                } else {
                    this.sendSignal(level, gameEvent, pos, blockpos);
                    return true;
                }
            }
        }
    }
}
