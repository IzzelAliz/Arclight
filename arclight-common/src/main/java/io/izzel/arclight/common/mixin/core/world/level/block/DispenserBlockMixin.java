package io.izzel.arclight.common.mixin.core.world.level.block;

import io.izzel.arclight.common.mod.mixins.annotation.TransformAccess;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DispenserBlock;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(DispenserBlock.class)
public class DispenserBlockMixin {

    @TransformAccess(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC)
    private static boolean eventFired = false;

    @TransformAccess(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC)
    private static DispenseItemBehavior getDispenseBehavior(BlockSource pointer, ItemStack stack) {
        return ((DispenserBlock) pointer.state().getBlock()).getDispenseMethod(pointer.level(), stack);
    }

    @Decorate(method = "dispenseFrom(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;levelEvent(ILnet/minecraft/core/BlockPos;I)V"), require = 1)
    private void arclight$failedDispense(ServerLevel level, int event, BlockPos pos, int data) throws Throwable {
        if (!CraftEventFactory.handleBlockFailedDispenseEvent(level, pos)) {
            // This void-host return also suppresses the following BLOCK_ACTIVATE game event.
            DecorationOps.cancel().invoke();
            return;
        }
        DecorationOps.callsite().invoke(level, event, pos, data);
    }

    @Decorate(method = "dispenseFrom(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/dispenser/DispenseItemBehavior;dispense(Lnet/minecraft/core/dispenser/BlockSource;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;"), require = 1)
    private ItemStack arclight$preDispense(DispenseItemBehavior behavior, BlockSource source, ItemStack stack,
                                         @Local(ordinal = 0) int slot) throws Throwable {
        if (!CraftEventFactory.handleBlockPreDispenseEvent(source.level(), source.pos(), stack, slot)) {
            // Cancel dispenseFrom, not just the behavior call, so setItem is not reached.
            return (ItemStack) DecorationOps.cancel().invoke();
        }
        eventFired = false;
        return (ItemStack) DecorationOps.callsite().invoke(behavior, source, stack);
    }
}
