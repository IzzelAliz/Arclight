package io.izzel.arclight.bridge.entity.passive;

import net.minecraft.item.ItemStack;
import io.izzel.arclight.bridge.entity.MobEntityBridge;

public interface AnimalEntityBridge extends MobEntityBridge {

    ItemStack bridge$getBreedItem();
}
