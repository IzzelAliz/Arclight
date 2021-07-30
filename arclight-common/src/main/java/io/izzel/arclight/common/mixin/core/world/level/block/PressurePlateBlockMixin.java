package io.izzel.arclight.common.mixin.core.world.level.block;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.Cancellable;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityInteractEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(PressurePlateBlock.class)
public abstract class PressurePlateBlockMixin extends BasePressurePlateBlockMixin {

    // @formatter:off
    @Shadow @Final private PressurePlateBlock.Sensitivity sensitivity;
    @Shadow protected abstract int getSignalForState(BlockState state);
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    protected int getSignalStrength(Level worldIn, BlockPos pos) {
        AABB axisalignedbb = TOUCH_AABB.move(pos);
        List<? extends Entity> list;
        switch (this.sensitivity) {
            case EVERYTHING:
                list = worldIn.getEntities(null, axisalignedbb);
                break;
            case MOBS:
                list = worldIn.getEntitiesOfClass(LivingEntity.class, axisalignedbb);
                break;
            default:
                return 0;
        }

        if (!list.isEmpty()) {
            for (Entity entity : list) {
                if (this.getSignalForState(worldIn.getBlockState(pos)) == 0) {
                    Cancellable cancellable;

                    if (entity instanceof Player) {
                        cancellable = CraftEventFactory.callPlayerInteractEvent((Player) entity, Action.PHYSICAL, pos, null, null, null);
                    } else {
                        cancellable = new EntityInteractEvent(((EntityBridge) entity).bridge$getBukkitEntity(), CraftBlock.at(worldIn, pos));
                        Bukkit.getPluginManager().callEvent((EntityInteractEvent) cancellable);
                    }

                    // We only want to block turning the plate on if all events are cancelled
                    if (cancellable.isCancelled()) {
                        continue;
                    }
                }
                if (!entity.isIgnoringBlockTriggers()) {
                    return 15;
                }
            }
        }

        return 0;
    }
}
