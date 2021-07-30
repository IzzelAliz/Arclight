package io.izzel.arclight.common.bridge.core.entity.passive;

import io.izzel.arclight.common.bridge.core.entity.MobEntityBridge;
import net.minecraft.world.item.ItemStack;

public interface AnimalEntityBridge extends MobEntityBridge {

    ItemStack bridge$getBreedItem();
}
