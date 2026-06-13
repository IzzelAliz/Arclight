package io.izzel.arclight.common.bridge.core.world.level.block.entity;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;

public interface AbstractFurnaceBlockEntityBridge {

    List<RecipeHolder<?>> bridge$dropExp(ServerPlayer entity, ItemStack itemStack, int amount);

    int bridge$getBurnDuration(ItemStack stack);

    boolean bridge$isLit();
}
