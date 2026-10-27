package io.izzel.arclight.neoforge.mixin.core.world.level.block;

import com.destroystokyo.paper.event.block.TNTPrimeEvent;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FireBlock.class)
@SuppressWarnings("removal")
public abstract class FireBlockLegacyTntPrimeEventMixin_NeoForge {

    @Decorate(method = "checkBurnOut(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;ILnet/minecraft/util/RandomSource;ILnet/minecraft/core/Direction;)V",
        at = @At(value = "INVOKE", remap = false,
            target = "Lnet/minecraft/world/level/block/state/BlockState;onCaughtFire(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Lnet/minecraft/world/entity/LivingEntity;)Z"),
        require = 1, allow = 1)
    private boolean arclight$legacyFireTntPrime(BlockState state, Level level, BlockPos pos,
                                              Direction face, LivingEntity igniter) throws Throwable {
        // NeoForge primes inside onCaughtFire, before its burnout RNG/write tail.
        // Keep the existing BlockBurnEvent/Bukkit TNTPrimeEvent before this producer,
        // and cancel the complete target burn before any loader priming callback.
        if (state.getBlock() instanceof TntBlock
            && !new TNTPrimeEvent(CraftBlock.at(level, pos), TNTPrimeEvent.PrimeReason.FIRE, null).callEvent()) {
            return (boolean) DecorationOps.cancel().invoke();
        }
        return (boolean) DecorationOps.callsite().invoke(state, level, pos, face, igniter);
    }
}
