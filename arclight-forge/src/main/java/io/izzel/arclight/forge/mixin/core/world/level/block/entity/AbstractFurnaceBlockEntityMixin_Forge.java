package io.izzel.arclight.forge.mixin.core.world.level.block.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import io.izzel.arclight.common.mod.server.event.ArclightEventFactory;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceBlockEntityMixin_Forge {

    // Wrap the transaction's entry, not burn's second list access (which is fuel
    // after output has already been written). The successor credit uses this same
    // invocation-local result, so cancellation cannot add recipe/experience credit.
    @WrapOperation(method = "serverTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;burn(Lnet/minecraft/core/NonNullList;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)V"))
    private static void arclight$furnaceSmelt(AbstractFurnaceBlockEntity furnace, NonNullList<ItemStack> items, ItemStack input, ItemStack result, Operation<Void> original,
                                             @Share(value = "smelted", namespace = "arclight:furnace") LocalBooleanRef smelted) {
        smelted.set(false);
        ItemStack replacement = ArclightEventFactory.smeltResult((net.minecraft.server.level.ServerLevel) furnace.getLevel(),
            furnace.getBlockPos(), items, input, result);
        if (replacement == null) {
            return;
        }
        // Native burn retains component-aware output, wet-sponge and input handling;
        // previous/next third-party wrappers still receive the accepted event result.
        original.call(furnace, items, input, replacement);
        smelted.set(true);
    }
}
