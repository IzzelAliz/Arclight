package io.izzel.arclight.common.mixin.core.item.crafting;

import io.izzel.arclight.common.bridge.item.crafting.IRecipeBridge;
import io.izzel.arclight.common.mod.util.ArclightSpecialRecipe;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import org.bukkit.inventory.Recipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(IRecipe.class)
public interface IRecipeMixin extends IRecipeBridge {

    // @formatter:off
    @Shadow ItemStack getRecipeOutput();
    // @formatter:on

    default Recipe toBukkitRecipe() {
        return bridge$toBukkitRecipe();
    }

    @Override
    default Recipe bridge$toBukkitRecipe() {
        return new ArclightSpecialRecipe((IRecipe<?>) this);
    }
}
