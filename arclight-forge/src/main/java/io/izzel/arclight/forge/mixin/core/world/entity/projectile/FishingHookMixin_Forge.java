package io.izzel.arclight.forge.mixin.core.world.entity.projectile;

import io.izzel.arclight.common.bridge.core.entity.projectile.FishingHookBridge;
import io.izzel.tools.product.Product;
import io.izzel.tools.product.Product2;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;

@Mixin(FishingHook.class)
public class FishingHookMixin_Forge implements FishingHookBridge {

    @Override
    public Product2<Boolean, Integer> bridge$forge$onItemFished(List<ItemStack> stacks, int rodDamage, FishingHook hook) {
        var event = new ItemFishedEvent(stacks, rodDamage, hook);
        MinecraftForge.EVENT_BUS.post(event);
        return Product.of(event.isCanceled(), event.getRodDamage());
    }
}
