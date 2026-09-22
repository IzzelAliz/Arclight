package io.izzel.arclight.common.mixin.core.world.level.block;

import io.izzel.arclight.common.bridge.core.world.level.WorldBridge;
import io.izzel.arclight.common.mod.util.FireDurationContext;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.InsideBlockEffectType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.LevelStem;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.EntityCombustByBlockEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BaseFireBlock.class)
public class BaseFireBlockMixin {

    // fireExtinguished implemented per class

    @Decorate(method = "entityInside(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/InsideBlockEffectApplier;Z)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/InsideBlockEffectApplier;apply(Lnet/minecraft/world/entity/InsideBlockEffectType;)V", ordinal = 1))
    private void arclight$onFire(InsideBlockEffectApplier applier, InsideBlockEffectType effect, BlockState blockState, Level level, BlockPos blockPos, Entity entity, InsideBlockEffectApplier originalApplier, boolean steppingCarefully) throws Throwable {
        var event = new EntityCombustByBlockEvent(CraftBlock.at(level, blockPos), entity.bridge$getBukkitEntity(), 8.0F);
        Bukkit.getPluginManager().callEvent(event);

        if (!event.isCancelled()) {
            applier.runBefore(effect, e -> FireDurationContext.set(e, event.getDuration()));
            DecorationOps.callsite().invoke(applier, effect);
        }
    }

    @Decorate(method = "fireIgnite", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;igniteForSeconds(F)V"))
    private static void arclight$fireIgniteDuration(Entity entity, float seconds) throws Throwable {
        DecorationOps.callsite().invoke(entity, FireDurationContext.consume(entity, seconds));
    }

    @Redirect(method = "onPlace", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;removeBlock(Lnet/minecraft/core/BlockPos;Z)Z"))
    public boolean arclight$extinguish2(Level world, BlockPos pos, boolean isMoving) {
        if (!CraftEventFactory.callBlockFadeEvent(world, pos, Blocks.AIR.defaultBlockState()).isCancelled()) {
            world.removeBlock(pos, isMoving);
        }
        return false;
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    private static boolean inPortalDimension(Level level) {
        var typeKey = ((WorldBridge) level).bridge$getTypeKey();
        return typeKey == LevelStem.NETHER || typeKey == LevelStem.OVERWORLD;
    }
}
