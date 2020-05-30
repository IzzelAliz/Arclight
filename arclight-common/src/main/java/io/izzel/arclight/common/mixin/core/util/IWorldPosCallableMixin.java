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
    static IWorldPosCallable of(final World p_221488_0_, final BlockPos p_221488_1_) {
        class Anonymous implements IWorldPosCallable, IWorldPosCallableBridge {

            @Override
            public <T> Optional<T> apply(BiFunction<World, BlockPos, T> p_221484_1_) {
                return Optional.of(p_221484_1_.apply(p_221488_0_, p_221488_1_));
            }

            @Override
            public World bridge$getWorld() {
                return p_221488_0_;
            }

            @Override
            public BlockPos bridge$getPosition() {
                return p_221488_1_;
            }
        }
        return new Anonymous();
    }
}
