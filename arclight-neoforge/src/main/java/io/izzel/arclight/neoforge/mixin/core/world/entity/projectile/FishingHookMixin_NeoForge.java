package io.izzel.arclight.neoforge.mixin.core.world.entity.projectile;

import io.izzel.arclight.common.bridge.core.entity.projectile.FishingHookBridge;
import io.izzel.tools.product.Product;
import io.izzel.tools.product.Product2;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;

@Mixin(FishingHook.class)
public abstract class FishingHookMixin_NeoForge implements FishingHookBridge {

    @Override
    public Product2<Boolean, Integer> bridge$forge$onItemFished(List<ItemStack> stacks, int rodDamage, FishingHook hook) {
        var event = NeoForge.EVENT_BUS.post(new ItemFishedEvent(stacks, rodDamage, hook));
        return Product.of(event.isCanceled(), event.getRodDamage());
    }
}
