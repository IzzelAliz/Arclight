package io.izzel.arclight.common.bridge.core.entity.monster.piglin;

import net.minecraft.world.item.Item;

import java.util.Set;

public interface PiglinBridge {

    Set<Item> bridge$getAllowedBarterItems();

    Set<Item> bridge$getInterestItems();
}
