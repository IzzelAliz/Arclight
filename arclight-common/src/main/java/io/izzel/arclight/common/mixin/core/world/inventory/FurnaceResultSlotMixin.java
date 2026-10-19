package io.izzel.arclight.common.mixin.core.world.inventory;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.izzel.arclight.common.mod.util.FurnaceEventHooks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.FurnaceResultSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FurnaceResultSlot.class)
public class FurnaceResultSlotMixin {

    // @formatter:off
    @Shadow private int removeCount;
    // @formatter:on

    @WrapOperation(method = "checkTakeAchievements(Lnet/minecraft/world/item/ItemStack;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;awardUsedRecipesAndPopExperience(Lnet/minecraft/server/level/ServerPlayer;)V"))
    public void arclight$furnaceDropExp(AbstractFurnaceBlockEntity furnace, ServerPlayer player, Operation<Void> original, ItemStack stack) {
        try (var scope = FurnaceEventHooks.beginExtraction(furnace.getBlockPos(), player, stack, this.removeCount)) {
            original.call(furnace, player);
        }
    }
}
