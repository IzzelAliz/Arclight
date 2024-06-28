package io.izzel.arclight.common.bridge.core.util;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public interface FoodStatsBridge {

    void bridge$setEntityHuman(Player playerEntity);

    Player bridge$getEntityHuman();

    void bridge$pushEatStack(ItemStack stack);
}
