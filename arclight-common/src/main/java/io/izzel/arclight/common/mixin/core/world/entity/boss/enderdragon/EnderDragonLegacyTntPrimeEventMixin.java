package io.izzel.arclight.common.mixin.core.world.entity.boss.enderdragon;

import com.destroystokyo.paper.event.block.TNTPrimeEvent;
import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Block;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// checkWalls is supplied by the existing priority-500 EnderDragonMixin overwrite.
// An INVOKE injection into another mixin's merged method requires a higher priority.
@Mixin(value = EnderDragon.class, priority = 600)
@SuppressWarnings("removal")
public abstract class EnderDragonLegacyTntPrimeEventMixin {

    @Decorate(method = "checkWalls(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/phys/AABB;)Z",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;wasExploded(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/Explosion;)V"),
        require = 1, allow = 1)
    private void arclight$legacyDragonTntPrime(Block block, ServerLevel level, BlockPos pos,
                                             Explosion explosion) throws Throwable {
        LivingEntity source = explosion.getIndirectSourceEntity();
        // Match Paper's nonzero-yield loop: this legacy event is not TNT-type-filtered.
        if (!new TNTPrimeEvent(CraftBlock.at(level, pos), TNTPrimeEvent.PrimeReason.EXPLOSION,
            source == null ? null : ((EntityBridge) source).bridge$getBukkitEntity()).callEvent()) {
            // Skip this block's wasExploded AND removeBlock, retaining prior drops and
            // continuing the edited EntityExplodeEvent block list rather than returning.
            throw DecorationOps.jumpToLoopStart();
        }
        DecorationOps.callsite().invoke(block, level, pos, explosion);
    }
}
