package io.izzel.arclight.common.mixin.core.world.level.block;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.server.level.ServerPlayerBridge;
import io.izzel.arclight.common.mixin.core.world.entity.EntityInsideBlockFrozenDataAccessor;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.papermc.paper.event.entity.EntityInsideBlockEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.PowderSnowBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(PowderSnowBlock.class)
public abstract class PowderSnowInsideBlockEventMixin {

    @Decorate(method = "entityInside(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/InsideBlockEffectApplier;Z)V",
        inject = true, at = @At("HEAD"), require = 1, allow = 1)
    private void arclight$powderSnowInsideBlock(BlockState state, Level level, BlockPos pos, Entity entity,
                                              InsideBlockEffectApplier effectApplier, boolean isPrecise) throws Throwable {
        if (!level.isClientSide()
            && !new EntityInsideBlockEvent(((EntityBridge) entity).bridge$getBukkitEntity(), CraftBlock.at(level, pos)).callEvent()) {
            if (entity instanceof ServerPlayer player) {
                var bukkitPlayer = ((ServerPlayerBridge) player).bridge$getBukkitEntity();
                if (bukkitPlayer.canSee(bukkitPlayer)) {
                    // Paper's targeted resend: include the current value even if
                    // default, without draining dirty or unrelated metadata.
                    var frozenTicks = EntityInsideBlockFrozenDataAccessor.arclight$insideBlockFrozenTicks();
                    player.connection.send(new ClientboundSetEntityDataPacket(player.getId(),
                        List.of(SynchedEntityData.DataValue.create(frozenTicks, player.getEntityData().get(frozenTicks)))));
                }
            }
            // No stuck multiplier, snow effects, melt callback or freeze/extinguish
            // work from this block may be queued after the event veto.
            DecorationOps.cancel().invoke();
            return;
        }
    }
}
