package io.izzel.arclight.common.mixin.core.util;

import io.izzel.arclight.common.bridge.util.IWorldPosCallableBridge;
import net.minecraft.util.IWorldPosCallable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.bukkit.Location;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.Optional;
import java.util.function.BiFunction;

@Mixin(IWorldPosCallable.class)
public interface IWorldPosCallableMixin extends IWorldPosCallableBridge {

    default World getWorld() {
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
    static IWorldPosCallable of(final World world, final BlockPos pos) {
        class Anonymous implements IWorldPosCallable, IWorldPosCallableBridge {

            @Override
            public <T> Optional<T> apply(BiFunction<World, BlockPos, T> worldPosConsumer) {
                return Optional.of(worldPosConsumer.apply(world, pos));
            }

            @Override
            public World bridge$getWorld() {
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
