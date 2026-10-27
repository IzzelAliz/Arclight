package io.izzel.arclight.neoforge.mixin.neoforge;

import io.izzel.arclight.common.mod.server.event.ArclightEventFactory.BlockDestruction;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Reuse the value already queried before BlockDestroyEvent, rather than sampling XP twice. */
@Mixin(BlockDropsEvent.class)
public abstract class BlockDropsEventDestructionExperienceMixin_NeoForge {

    @Decorate(method = "<init>", remap = false,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getExpDrop(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)I"), require = 1, allow = 1)
    private int arclight$reuseDestructionExperience(BlockState state, LevelAccessor level, BlockPos pos,
                                                   BlockEntity blockEntity, Entity breaker, ItemStack tool) throws Throwable {
        BlockDestruction context = BlockDestruction.current(level, pos);
        if (context != null && !context.isBlockBreak()) {
            return context.experience();
        }
        return (int) DecorationOps.callsite().invoke(state, level, pos, blockEntity, breaker, tool);
    }

    @Decorate(method = "<init>", remap = false,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;processBlockExperience(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;I)I"), require = 1, allow = 1)
    private int arclight$reuseDestructionEnchantments(ServerLevel level, ItemStack tool, int experience,
                                                      @Local(ordinal = 0) BlockPos pos) throws Throwable {
        BlockDestruction context = BlockDestruction.current(level, pos);
        if (context != null && !context.isBlockBreak()) {
            return context.experience();
        }
        return (int) DecorationOps.callsite().invoke(level, tool, experience);
    }
}
