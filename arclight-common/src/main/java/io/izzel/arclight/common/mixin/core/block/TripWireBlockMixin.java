package io.izzel.arclight.common.mixin.core.block;

import io.izzel.arclight.common.bridge.entity.EntityBridge;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.TripWireBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.state.BooleanProperty;
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

@Mixin(TripWireBlock.class)
public abstract class TripWireBlockMixin extends BlockMixin {

    // @formatter:off
    @Shadow @Final public static BooleanProperty POWERED;
    @Shadow protected abstract void notifyHook(World worldIn, BlockPos pos, BlockState state);
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    private void updateState(World worldIn, BlockPos pos) {
        BlockState blockstate = worldIn.getBlockState(pos);
        boolean flag = blockstate.get(POWERED);
        boolean flag1 = false;
        List<? extends Entity> list = worldIn.getEntitiesWithinAABBExcludingEntity(null, blockstate.getShape(worldIn, pos).getBoundingBox().offset(pos));
        if (!list.isEmpty()) {
            for (Entity entity : list) {
                if (!entity.doesEntityNotTriggerPressurePlate()) {
                    flag1 = true;
                    break;
                }
            }
        }

        if (flag != flag1 && flag1 && blockstate.get(TripWireBlock.ATTACHED)) {
            org.bukkit.block.Block block = CraftBlock.at(worldIn, pos);
            boolean allowed = false;

            // If all of the events are cancelled block the tripwire trigger, else allow
            for (Object object : list) {
                if (object != null) {
                    Cancellable cancellable;

                    if (object instanceof PlayerEntity) {
                        cancellable = CraftEventFactory.callPlayerInteractEvent((PlayerEntity) object, Action.PHYSICAL, pos, null, null, null);
                    } else if (object instanceof Entity) {
                        cancellable = new EntityInteractEvent(((EntityBridge) object).bridge$getBukkitEntity(), block);
                        Bukkit.getPluginManager().callEvent((EntityInteractEvent) cancellable);
                    } else {
                        continue;
                    }

                    if (!cancellable.isCancelled()) {
                        allowed = true;
                        break;
                    }
                }
            }

            if (!allowed) {
                return;
            }
        }

        if (flag1 != flag) {
            blockstate = blockstate.with(POWERED, flag1);
            worldIn.setBlockState(pos, blockstate, 3);
            this.notifyHook(worldIn, pos, blockstate);
        }

        if (flag1) {
            worldIn.getPendingBlockTicks().scheduleTick(new BlockPos(pos), (Block) (Object) this, 10);
        }

    }
}
