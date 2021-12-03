package io.izzel.arclight.common.mixin.core.world.level.block;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.TripWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
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
    @Shadow protected abstract void updateSource(Level worldIn, BlockPos pos, BlockState state);
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    private void checkPressed(Level worldIn, BlockPos pos) {
        BlockState blockstate = worldIn.getBlockState(pos);
        boolean flag = blockstate.getValue(POWERED);
        boolean flag1 = false;
        List<? extends Entity> list = worldIn.getEntities(null, blockstate.getShape(worldIn, pos).bounds().move(pos));
        if (!list.isEmpty()) {
            for (Entity entity : list) {
                if (!entity.isIgnoringBlockTriggers()) {
                    flag1 = true;
                    break;
                }
            }
        }

        if (flag != flag1 && flag1 && blockstate.getValue(TripWireBlock.ATTACHED)) {
            org.bukkit.block.Block block = CraftBlock.at(worldIn, pos);
            boolean allowed = false;

            // If all of the events are cancelled block the tripwire trigger, else allow
            for (Object object : list) {
                if (object != null) {
                    Cancellable cancellable;

                    if (object instanceof Player) {
                        cancellable = CraftEventFactory.callPlayerInteractEvent((Player) object, Action.PHYSICAL, pos, null, null, null);
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
            blockstate = blockstate.setValue(POWERED, flag1);
            worldIn.setBlock(pos, blockstate, 3);
            this.updateSource(worldIn, pos, blockstate);
        }

        if (flag1) {
            worldIn.scheduleTick(new BlockPos(pos), (Block) (Object) this, 10);
        }

    }
}
