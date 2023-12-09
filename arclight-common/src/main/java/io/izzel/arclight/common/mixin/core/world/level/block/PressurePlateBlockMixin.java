package io.izzel.arclight.common.mixin.core.world.level.block;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.world.WorldBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityInteractEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(PressurePlateBlock.class)
public abstract class PressurePlateBlockMixin extends BasePressurePlateBlockMixin {

    // @formatter:off
    @Shadow protected abstract int getSignalForState(BlockState state);
    // @formatter:on

    private static <T extends Entity> java.util.List<T> getEntities(Level world, AABB axisalignedbb, Class<T> oclass) {
        return world.getEntitiesOfClass(oclass, axisalignedbb, EntitySelector.NO_SPECTATORS.and((entity) -> {
            return !entity.isIgnoringBlockTriggers();
        }));
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    protected int getSignalStrength(Level world, BlockPos blockposition) {
        Class<? extends Entity> oclass; // CraftBukkit

        switch (this.type.pressurePlateSensitivity()) {
            case EVERYTHING:
                oclass = Entity.class;
                break;
            case MOBS:
                oclass = LivingEntity.class;
                break;
            default:
                throw new IncompatibleClassChangeError();
        }

        Class oclass1 = oclass;

        // CraftBukkit start - Call interact event when turning on a pressure plate
        for (Entity entity : getEntities(world, TOUCH_AABB.move(blockposition), oclass)) {
            if (this.getSignalForState(world.getBlockState(blockposition)) == 0) {
                org.bukkit.World bworld = ((WorldBridge) world).bridge$getWorld();
                org.bukkit.plugin.PluginManager manager = Bukkit.getPluginManager();
                org.bukkit.event.Cancellable cancellable;

                if (entity instanceof Player) {
                    cancellable = CraftEventFactory.callPlayerInteractEvent((Player) entity, Action.PHYSICAL, blockposition, null, null, null);
                } else {
                    cancellable = new EntityInteractEvent(((EntityBridge) entity).bridge$getBukkitEntity(), bworld.getBlockAt(blockposition.getX(), blockposition.getY(), blockposition.getZ()));
                    manager.callEvent((EntityInteractEvent) cancellable);
                }
                // We only want to block turning the plate on if all events are cancelled
                if (cancellable.isCancelled()) {
                    continue;
                }
            }

            return 15;
        }

        return 0;
    }
}
