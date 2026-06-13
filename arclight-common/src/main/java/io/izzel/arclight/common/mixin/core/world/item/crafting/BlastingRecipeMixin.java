package io.izzel.arclight.common.mixin.core.world.item.crafting;

import io.izzel.arclight.common.bridge.core.world.item.crafting.RecipeBridge;
import io.izzel.arclight.common.mod.util.ArclightSpecialRecipe;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.inventory.CraftBlastingRecipe;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.inventory.CraftRecipe;
import org.bukkit.inventory.Recipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BlastingRecipe.class)
public abstract class BlastingRecipeMixin implements RecipeBridge {

    @Shadow protected abstract ItemStackTemplate result();
    @Shadow public abstract Ingredient input();

    @Override
    public Recipe bridge$toBukkitRecipe(NamespacedKey id) {
        if (this.result().count() == 0) {
            return new ArclightSpecialRecipe(id, (BlastingRecipe) (Object) this);
        }
        BlastingRecipe recipe = (BlastingRecipe) (Object) this;
        CraftBlastingRecipe bukkit = new CraftBlastingRecipe(id, CraftItemStack.asCraftMirror(this.result().create()), CraftRecipe.toBukkit(this.input()), recipe.experience(), recipe.cookingTime());
        bukkit.setGroup(recipe.group());
        bukkit.setCategory(CraftRecipe.getCategory(recipe.category()));
        return bukkit;
    }
}
