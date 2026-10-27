package io.izzel.arclight.common.bridge.core.world.entity.projectile;

import io.izzel.tools.product.Product;
import io.izzel.tools.product.Product2;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public interface FishingHookBridge {

    void bridge$resetTimeUntilLured();

    net.minecraft.world.InteractionHand bridge$getRetrieveHand();

    net.minecraft.world.InteractionHand bridge$swapRetrieveHand(net.minecraft.world.InteractionHand hand);

    default Product2<Boolean, Integer> bridge$forge$onItemFished(List<ItemStack> stacks, int rodDamage, FishingHook hook) {
        return Product.of(false, rodDamage);
    }

    default void resetTimeUntilLured() {
        this.bridge$resetTimeUntilLured();
    }

}
