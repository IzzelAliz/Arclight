package io.izzel.arclight.neoforge.mixin.core.world.item.crafting;

import io.izzel.arclight.common.bridge.core.world.item.crafting.IngredientBridge;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Ingredient.class)
public abstract class IngredientMixin_NeoForge implements IngredientBridge {

    @Shadow public abstract boolean isCustom();

    @Override
    public boolean bridge$isCustom() {
        return this.isCustom();
    }
}
