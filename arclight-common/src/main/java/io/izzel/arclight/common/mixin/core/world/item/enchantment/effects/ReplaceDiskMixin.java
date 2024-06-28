package io.izzel.arclight.common.mixin.core.world.item.enchantment.effects;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.effects.ReplaceDisk;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ReplaceDisk.class)
public class ReplaceDiskMixin {

    @Redirect(method = "apply", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean arclight$changeBlock(ServerLevel instance, BlockPos pos, BlockState blockState, ServerLevel serverLevel, int i, EnchantedItemInUse enchantedItemInUse, Entity entity) {
        return CraftEventFactory.handleBlockFormEvent(instance, pos, blockState, entity);
    }
}
