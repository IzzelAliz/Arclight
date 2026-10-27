package io.izzel.arclight.common.mixin.vanilla.world.level.block;

import com.destroystokyo.paper.event.block.TNTPrimeEvent;
import io.izzel.arclight.api.ArclightPlatform;
import io.izzel.arclight.common.mod.mixins.annotation.OnlyInPlatform;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@OnlyInPlatform({ArclightPlatform.VANILLA, ArclightPlatform.FABRIC})
@Mixin(FireBlock.class)
@SuppressWarnings("removal")
public abstract class FireBlockLegacyTntPrimeEventMixin_Vanilla {

    @Decorate(method = "checkBurnOut(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;ILnet/minecraft/util/RandomSource;I)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;removeBlock(Lnet/minecraft/core/BlockPos;Z)Z"),
        require = 1, allow = 1)
    private boolean arclight$deferLegacyTntRemoval(Level level, BlockPos pos, boolean moving,
                                                @Local(ordinal = 0) BlockState oldState) throws Throwable {
        // Paper defers this branch's TNT removal until the legacy event allows priming.
        return !oldState.is(Blocks.TNT)
            && (boolean) DecorationOps.callsite().invoke(level, pos, moving);
    }

    @Decorate(method = "checkBurnOut(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;ILnet/minecraft/util/RandomSource;I)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/TntBlock;prime(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Z"),
        require = 1, allow = 1)
    private boolean arclight$legacyFireTntPrime(Level level, BlockPos pos) throws Throwable {
        if (!new TNTPrimeEvent(CraftBlock.at(level, pos), TNTPrimeEvent.PrimeReason.FIRE, null).callEvent()) {
            // checkBurnOut is void: cancel its tail, not just the boolean prime call.
            return (boolean) DecorationOps.cancel().invoke();
        }
        level.removeBlock(pos, false);
        return (boolean) DecorationOps.callsite().invoke(level, pos);
    }
}
