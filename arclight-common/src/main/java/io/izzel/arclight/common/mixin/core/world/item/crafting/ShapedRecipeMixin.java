package io.izzel.arclight.common.mixin.core.world.item.crafting;

import io.izzel.arclight.common.bridge.core.world.item.crafting.RecipeBridge;
import io.izzel.arclight.common.mod.util.ArclightSpecialRecipe;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.inventory.CraftRecipe;
import org.bukkit.craftbukkit.inventory.CraftShapedRecipe;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Optional;

@Mixin(ShapedRecipe.class)
public abstract class ShapedRecipeMixin implements RecipeBridge {

    // @formatter:off
    @Shadow @Final ShapedRecipePattern pattern;
    @Shadow @Final ItemStackTemplate result;
    @Shadow public abstract int getHeight();
    @Shadow public abstract int getWidth();
    // @formatter:on

    @Override
    public Recipe bridge$toBukkitRecipe(NamespacedKey id) {
        if (this.getWidth() < 1 || this.getWidth() > 3 || this.getHeight() < 1 || this.getHeight() > 3) {
            return new ArclightSpecialRecipe(id, (net.minecraft.world.item.crafting.Recipe<?>) this);
        }
        var output = this.result.create();
        if (output.isEmpty()) {
            return new ArclightSpecialRecipe(id, (net.minecraft.world.item.crafting.Recipe<?>) this);
        }
        CraftItemStack result = CraftItemStack.asCraftMirror(output);
        CraftShapedRecipe recipe = new CraftShapedRecipe(id, result, (ShapedRecipe) (Object) this);
        // Metadata getters are inherited from NormalCraftingRecipe, not declared shadow targets.
        ShapedRecipe self = (ShapedRecipe) (Object) this;
        recipe.setGroup(self.group());
        recipe.setCategory(CraftRecipe.getCategory(self.category()));

        switch (this.getHeight()) {
            case 1:
                switch (this.getWidth()) {
                    case 1:
                        recipe.shape("a");
                        break;
                    case 2:
                        recipe.shape("ab");
                        break;
                    case 3:
                        recipe.shape("abc");
                        break;
                }
                break;
            case 2:
                switch (this.getWidth()) {
                    case 1:
                        recipe.shape("a", "b");
                        break;
                    case 2:
                        recipe.shape("ab", "cd");
                        break;
                    case 3:
                        recipe.shape("abc", "def");
                        break;
                }
                break;
            case 3:
                switch (this.getWidth()) {
                    case 1:
                        recipe.shape("a", "b", "c");
                        break;
                    case 2:
                        recipe.shape("ab", "cd", "ef");
                        break;
                    case 3:
                        recipe.shape("abc", "def", "ghi");
                        break;
                }
                break;
        }
        char c = 'a';
        for (Optional<Ingredient> optional : this.pattern.ingredients()) {
            if (optional.isPresent()) {
                RecipeChoice choice = CraftRecipe.toChoice(optional.get());
                if (choice != null && choice != RecipeChoice.empty()) {
                    recipe.setIngredient(c, choice);
                }
            }

            c++;
        }
        return recipe;
    }
}
