package io.izzel.arclight.common.mixin.core.world.level.block;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.papermc.paper.configuration.GlobalConfiguration;
import io.papermc.paper.event.entity.EntityInsideBlockEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TripWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TripWireBlock.class)
public abstract class TripWireInsideBlockEventMixin {

    @Decorate(method = "entityInside(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/InsideBlockEffectApplier;Z)V",
        inject = true, at = @At("HEAD"), require = 1, allow = 1)
    private void arclight$tripWireInsideBlock(BlockState state, Level level, BlockPos pos, Entity entity,
                                            InsideBlockEffectApplier effectApplier, boolean isPrecise) throws Throwable {
        if (!level.isClientSide()) {
            // Paper suppresses this producer altogether when tripwire collision
            // detection is disabled, before dispatching EntityInsideBlockEvent.
            if (GlobalConfiguration.get().blockUpdates.disableTripwireUpdates) {
                DecorationOps.cancel().invoke();
                return;
            }
            if (!new EntityInsideBlockEvent(((EntityBridge) entity).bridge$getBukkitEntity(), CraftBlock.at(level, pos)).callEvent()) {
                DecorationOps.cancel().invoke();
                return;
            }
        }
    }
}
