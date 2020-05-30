package io.izzel.arclight.common.mixin.core.block;

import io.izzel.arclight.common.bridge.entity.EntityBridge;
import net.minecraft.block.BlockState;
import net.minecraft.block.PressurePlateBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
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
public abstract class PressurePlateBlockMixin extends AbstractPressurePlateBlockMixin {

    // @formatter:off
    @Shadow @Final private PressurePlateBlock.Sensitivity sensitivity;
    @Shadow protected abstract int getRedstoneStrength(BlockState state);
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    protected int computeRedstoneStrength(World worldIn, BlockPos pos) {
        AxisAlignedBB axisalignedbb = PRESSURE_AABB.offset(pos);
        List<? extends Entity> list;
        switch (this.sensitivity) {
            case EVERYTHING:
                list = worldIn.getEntitiesWithinAABBExcludingEntity(null, axisalignedbb);
                break;
            case MOBS:
                list = worldIn.getEntitiesWithinAABB(LivingEntity.class, axisalignedbb);
                break;
            default:
                return 0;
        }

        if (!list.isEmpty()) {
            for (Entity entity : list) {
                if (this.getRedstoneStrength(worldIn.getBlockState(pos)) == 0) {
                    Cancellable cancellable;

                    if (entity instanceof PlayerEntity) {
                        cancellable = CraftEventFactory.callPlayerInteractEvent((PlayerEntity) entity, Action.PHYSICAL, pos, null, null, null);
                    } else {
                        cancellable = new EntityInteractEvent(((EntityBridge) entity).bridge$getBukkitEntity(), CraftBlock.at(worldIn, pos));
                        Bukkit.getPluginManager().callEvent((EntityInteractEvent) cancellable);
                    }

                    // We only want to block turning the plate on if all events are cancelled
                    if (cancellable.isCancelled()) {
                        continue;
                    }
                }
                if (!entity.doesEntityNotTriggerPressurePlate()) {
                    return 15;
                }
            }
        }

        return 0;
    }
}
