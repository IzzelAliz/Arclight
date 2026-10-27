package io.izzel.arclight.common.bridge.core.world.level.block.entity;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;

public interface AbstractFurnaceBlockEntityBridge {

    net.minecraft.world.item.crafting.RecipeType<? extends net.minecraft.world.item.crafting.AbstractCookingRecipe> bridge$getRecipeType();

    List<RecipeHolder<?>> bridge$dropExp(ServerPlayer entity, ItemStack itemStack, int amount);

    int bridge$getBurnDuration(ItemStack stack);

    boolean bridge$isLit();
}
