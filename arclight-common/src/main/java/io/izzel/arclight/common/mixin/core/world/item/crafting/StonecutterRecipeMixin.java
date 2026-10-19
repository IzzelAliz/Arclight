package io.izzel.arclight.common.mixin.core.world.item.crafting;

import io.izzel.arclight.common.bridge.core.world.item.crafting.RecipeBridge;
import io.izzel.arclight.common.mod.util.ArclightSpecialRecipe;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Recipe.CommonInfo;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SingleItemRecipe;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.inventory.CraftRecipe;
import org.bukkit.craftbukkit.inventory.CraftStonecuttingRecipe;
import org.bukkit.inventory.Recipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(StonecutterRecipe.class)
public abstract class StonecutterRecipeMixin extends SingleItemRecipe implements RecipeBridge {

    @Shadow public abstract String group();

    public StonecutterRecipeMixin(CommonInfo commonInfo, Ingredient input, ItemStackTemplate result) {
        super(commonInfo, input, result);
    }

    @Override
    public Recipe bridge$toBukkitRecipe(NamespacedKey id) {
        if (this.result().create().isEmpty()) {
            return new ArclightSpecialRecipe(id, this);
        }
        CraftItemStack result = CraftItemStack.asCraftMirror(this.result());
        CraftStonecuttingRecipe recipe = new CraftStonecuttingRecipe(id, result, CraftRecipe.toBukkit(this.input()));
        recipe.setGroup(this.group());
        return recipe;
    }
}
