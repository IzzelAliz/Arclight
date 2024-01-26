package io.izzel.arclight.forge.mixin.core.world.entity.monster.piglin;

import io.izzel.arclight.common.bridge.core.entity.monster.piglin.PiglinBridge;
import io.izzel.arclight.forge.mixin.core.world.entity.LivingEntityMixin_Forge;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Piglin.class)
public abstract class PiglinMixin_Forge extends LivingEntityMixin_Forge implements PiglinBridge {

    @Redirect(method = "holdInOffHand", at = @At(value = "INVOKE", remap = false, target = "Lnet/minecraft/world/item/ItemStack;isPiglinCurrency()Z"))
    private boolean arclight$customBarter(ItemStack itemStack) {
        return itemStack.isPiglinCurrency() || bridge$getAllowedBarterItems().contains(itemStack.getItem());
    }
}
