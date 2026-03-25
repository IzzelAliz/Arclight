package io.izzel.arclight.common.bridge.core.world.entity.animal;

import io.izzel.arclight.common.bridge.core.world.entity.MobBridge;
import net.minecraft.world.item.ItemStack;

public interface AnimalBridge extends MobBridge {

    ItemStack bridge$getBreedItem();
}
