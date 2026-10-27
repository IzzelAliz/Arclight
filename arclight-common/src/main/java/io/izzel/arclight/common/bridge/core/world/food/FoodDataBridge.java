package io.izzel.arclight.common.bridge.core.world.food;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public interface FoodDataBridge {

    void bridge$setEntityHuman(Player playerEntity);

    Player bridge$getEntityHuman();

    void bridge$pushEatStack(ItemStack stack);

    int bridge$getSaturatedRegenRate();

    void bridge$setSaturatedRegenRate(int rate);

    int bridge$getUnsaturatedRegenRate();

    void bridge$setUnsaturatedRegenRate(int rate);

    int bridge$getStarvationRate();

    void bridge$setStarvationRate(int rate);

}
