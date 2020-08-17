package io.izzel.arclight.common.mixin.core.item.crafting;

import io.izzel.arclight.common.bridge.item.crafting.IRecipeBridge;
import net.minecraft.item.crafting.SpecialRecipe;
import org.bukkit.inventory.Recipe;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(SpecialRecipe.class)
public class SpecialRecipeMixin implements IRecipeBridge {

    @Override
    public Recipe bridge$toBukkitRecipe() {
        return new org.bukkit.craftbukkit.v.inventory.CraftComplexRecipe((SpecialRecipe) (Object) this);
    }
}
