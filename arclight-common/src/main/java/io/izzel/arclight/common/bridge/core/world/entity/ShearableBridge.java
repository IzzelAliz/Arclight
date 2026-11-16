package io.izzel.arclight.common.bridge.core.world.entity;

import net.minecraft.world.item.ItemStack;

import java.util.List;

public interface ShearableBridge {

    List<ItemStack> bridge$swapShearDrops(List<ItemStack> drops);
}
