package io.izzel.arclight.common.mixin.core.world.item.enchantment.effects;

import io.izzel.arclight.common.mod.server.event.ArclightEventFactory;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.effects.ReplaceBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.block.CraftBlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ReplaceBlock.class)
public class ReplaceBlockMixin {

    @Decorate(method = "apply", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean arclight$changeBlock(ServerLevel instance, BlockPos pos, BlockState newState, ServerLevel level, int i, EnchantedItemInUse enchantedItemInUse, Entity entity) throws Throwable {
        final var event = ArclightEventFactory.callBlockFormEvent(level, pos, newState, 3, entity);
        if (event != null) {
            if (event.isCancelled()) {
                return false;
            }
            newState = ((CraftBlockState) event.getNewState()).getHandle();
        }
        return (boolean) DecorationOps.callsite().invoke(level, pos, newState);
    }
}
