package io.izzel.arclight.mixin.core.item.crafting;

import io.izzel.arclight.bridge.item.crafting.IRecipeBridge;
import net.minecraft.item.crafting.IRecipe;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.spongepowered.asm.mixin.Mixin;
import io.izzel.arclight.mod.util.ArclightSpecialRecipe;

@Mixin(IRecipe.class)
public interface IRecipeMixin extends IRecipeBridge {

    default Recipe toBukkitRecipe() {
        return bridge$toBukkitRecipe();
    }

    @Override
    default Recipe bridge$toBukkitRecipe() {
        return ArclightSpecialRecipe.shapeless(new ItemStack(Material.AIR), (IRecipe<?>) this);
    }
}
