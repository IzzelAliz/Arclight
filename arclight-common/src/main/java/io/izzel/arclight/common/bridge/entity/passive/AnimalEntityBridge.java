package io.izzel.arclight.common.bridge.entity.passive;

import net.minecraft.item.ItemStack;
import io.izzel.arclight.common.bridge.entity.MobEntityBridge;

public interface AnimalEntityBridge extends MobEntityBridge {

    ItemStack bridge$getBreedItem();
}
