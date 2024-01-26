package io.izzel.arclight.common.bridge.core.util;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;

public interface FoodStatsBridge {

    void bridge$setEntityHuman(Player playerEntity);

    Player bridge$getEntityHuman();

    default FoodProperties bridge$forge$getFoodProperties(ItemStack stack, LivingEntity entity) {
        return stack.getItem().getFoodProperties();
    }
}
