package io.izzel.arclight.common.mixin.core.world.inventory;

import io.izzel.arclight.common.bridge.core.util.IWorldPosCallableBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import org.bukkit.Location;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.Optional;
import java.util.function.BiFunction;

@Mixin(ContainerLevelAccess.class)
public interface ContainerLevelAccessMixin extends IWorldPosCallableBridge {

    default Level getWorld() {
        return bridge$getWorld();
    }

    default BlockPos getPosition() {
        return bridge$getPosition();
    }

    default Location getLocation() {
        return bridge$getLocation();
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    static ContainerLevelAccess create(final Level world, final BlockPos pos) {
        class Anonymous implements ContainerLevelAccess, IWorldPosCallableBridge {

            @Override
            public <T> Optional<T> evaluate(BiFunction<Level, BlockPos, T> worldPosConsumer) {
                return Optional.of(worldPosConsumer.apply(world, pos));
            }

            @Override
            public Level bridge$getWorld() {
                return world;
            }

            @Override
            public BlockPos bridge$getPosition() {
                return pos;
            }
        }
        return new Anonymous();
    }
}
