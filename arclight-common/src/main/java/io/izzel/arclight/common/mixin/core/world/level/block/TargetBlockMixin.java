package io.izzel.arclight.common.mixin.core.world.level.block;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import io.papermc.paper.event.block.TargetHitEvent;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.TargetBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TargetBlock.class)
public abstract class TargetBlockMixin {

    @Decorate(method = "onProjectileHit(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/phys/BlockHitResult;Lnet/minecraft/world/entity/projectile/Projectile;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/TargetBlock;updateRedstoneOutput(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/phys/BlockHitResult;Lnet/minecraft/world/entity/Entity;)I"), require = 1, allow = 1)
    private int arclight$moveTargetHitCriteria(LevelAccessor level, BlockState state, BlockHitResult hitResult, Entity entity) throws Throwable {
        int outputStrength = (int) DecorationOps.callsite().invoke(level, state, hitResult, entity);
        // The update now awards accepted hits, including calls while a reset tick is pending.
        // End the void host so its old unconditional stat/criteria tail cannot award twice or on veto.
        return (int) DecorationOps.cancel().invoke();
    }

    @Decorate(method = "updateRedstoneOutput(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/phys/BlockHitResult;Lnet/minecraft/world/entity/Entity;)I", inject = true,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/LevelAccessor;getBlockTicks()Lnet/minecraft/world/ticks/LevelTickAccess;"), require = 1, allow = 1)
    private static void arclight$targetHit(LevelAccessor level, BlockState state, BlockHitResult hitResult, Entity entity,
                                         @Local(ordinal = 0) int redstoneStrength) throws Throwable {
        if (entity instanceof Projectile) {
            TargetHitEvent event = new TargetHitEvent((org.bukkit.entity.Projectile) ((EntityBridge) entity).bridge$getBukkitEntity(),
                CraftBlock.at(level, hitResult.getBlockPos()), CraftBlock.notchToBlockFace(hitResult.getDirection()), redstoneStrength);
            if (!event.callEvent()) {
                // Return the unedited native strength, without a block write, reset tick or award.
                DecorationOps.cancel().invoke(redstoneStrength);
                return;
            }
            redstoneStrength = event.getSignalStrength();
            DecorationOps.blackhole().invoke(redstoneStrength);
        }
    }

    @Decorate(method = "updateRedstoneOutput(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/phys/BlockHitResult;Lnet/minecraft/world/entity/Entity;)I", inject = true,
        at = @At("RETURN"), require = 1, allow = 1)
    private static void arclight$awardTargetHitCriteria(LevelAccessor level, BlockState state, BlockHitResult hitResult, Entity entity,
                                                      @Local(ordinal = 0) int redstoneStrength) {
        if (entity instanceof Projectile projectile && projectile.getOwner() instanceof ServerPlayer playerOwner) {
            playerOwner.awardStat(Stats.TARGET_HIT);
            CriteriaTriggers.TARGET_BLOCK_HIT.trigger(playerOwner, projectile, hitResult.getLocation(), redstoneStrength);
        }
    }
}
