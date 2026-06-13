package io.izzel.arclight.common.mixin.core.world.item.crafting;

import io.izzel.arclight.common.bridge.core.world.item.crafting.RecipeBridge;
import io.izzel.arclight.common.mod.util.ArclightSpecialRecipe;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.inventory.CraftFurnaceRecipe;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.inventory.CraftRecipe;
import org.bukkit.inventory.Recipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(SmeltingRecipe.class)
public abstract class SmeltingRecipeMixin implements RecipeBridge {

    @Shadow protected abstract ItemStackTemplate result();
    @Shadow public abstract Ingredient input();

    @Override
    public Recipe bridge$toBukkitRecipe(NamespacedKey id) {
        if (this.result().count() == 0) {
            return new ArclightSpecialRecipe(id, (SmeltingRecipe) (Object) this);
        }
        SmeltingRecipe recipe = (SmeltingRecipe) (Object) this;
        CraftFurnaceRecipe bukkit = new CraftFurnaceRecipe(id, CraftItemStack.asCraftMirror(this.result().create()), CraftRecipe.toBukkit(this.input()), recipe.experience(), recipe.cookingTime());
        bukkit.setGroup(recipe.group());
        bukkit.setCategory(CraftRecipe.getCategory(recipe.category()));
        return bukkit;
    }
}
