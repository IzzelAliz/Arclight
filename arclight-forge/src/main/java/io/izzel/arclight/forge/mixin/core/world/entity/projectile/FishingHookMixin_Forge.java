package io.izzel.arclight.forge.mixin.core.world.entity.projectile;

import io.izzel.arclight.common.bridge.core.world.entity.projectile.FishingHookBridge;
import io.izzel.tools.product.Product;
import io.izzel.tools.product.Product2;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;

@Mixin(FishingHook.class)
public abstract class FishingHookMixin_Forge implements FishingHookBridge {

    @Override
    public Product2<Boolean, Integer> bridge$forge$onItemFished(List<ItemStack> stacks, int rodDamage, FishingHook hook) {
        var event = new ItemFishedEvent(stacks, rodDamage, hook);
        boolean cancelled = ItemFishedEvent.BUS.post(event);
        return Product.of(cancelled, event.getRodDamage());
    }
}
