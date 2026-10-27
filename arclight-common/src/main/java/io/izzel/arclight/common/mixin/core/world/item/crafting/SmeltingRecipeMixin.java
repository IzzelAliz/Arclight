package io.izzel.arclight.common.mixin.core.world.item.crafting;

import io.izzel.arclight.common.bridge.core.world.item.crafting.RecipeBridge;
import io.izzel.arclight.common.mod.util.ArclightSpecialRecipe;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Recipe.CommonInfo;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.inventory.CraftFurnaceRecipe;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.inventory.CraftRecipe;
import org.bukkit.inventory.Recipe;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(SmeltingRecipe.class)
public abstract class SmeltingRecipeMixin extends AbstractCookingRecipe implements RecipeBridge {

    public SmeltingRecipeMixin(CommonInfo commonInfo, CookingBookInfo bookInfo, Ingredient input, ItemStackTemplate result, float experience, int cookingTime) {
        super(commonInfo, bookInfo, input, result, experience, cookingTime);
    }

    @Override
    public Recipe bridge$toBukkitRecipe(NamespacedKey id) {
        var output = this.result().create();
        if (output.isEmpty()) {
            return new ArclightSpecialRecipe(id, this);
        }
        CraftItemStack result = CraftItemStack.asCraftMirror(output);

        CraftFurnaceRecipe recipe = new CraftFurnaceRecipe(id, result, CraftRecipe.toChoice(this.input()), this.experience(), this.cookingTime());
        recipe.setGroup(this.group());
        recipe.setCategory(CraftRecipe.getCategory(this.category()));

        return recipe;
    }
}
