package io.izzel.arclight.common.mixin.core.world.item.crafting;

import io.izzel.arclight.common.bridge.core.world.item.crafting.RecipeBridge;
import io.izzel.arclight.common.mod.util.ArclightSpecialRecipe;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.inventory.CraftRecipe;
import org.bukkit.craftbukkit.inventory.CraftStonecuttingRecipe;
import org.bukkit.inventory.Recipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(StonecutterRecipe.class)
public abstract class StonecutterRecipeMixin implements RecipeBridge {

    @Shadow protected abstract ItemStackTemplate result();
    @Shadow public abstract Ingredient input();

    @Override
    public Recipe bridge$toBukkitRecipe(NamespacedKey id) {
        if (this.result().count() == 0) {
            return new ArclightSpecialRecipe(id, (StonecutterRecipe) (Object) this);
        }
        StonecutterRecipe recipe = (StonecutterRecipe) (Object) this;
        CraftStonecuttingRecipe bukkit = new CraftStonecuttingRecipe(id, CraftItemStack.asCraftMirror(this.result().create()), CraftRecipe.toBukkit(this.input()));
        bukkit.setGroup(recipe.group());
        return bukkit;
    }
}
