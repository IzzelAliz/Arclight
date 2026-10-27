package io.izzel.arclight.common.mixin.core.world.item.crafting;

import io.izzel.arclight.common.bridge.core.world.item.crafting.RecipeBridge;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.DyeRecipe;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.inventory.CraftComplexRecipe;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.inventory.CraftRecipe;
import org.bukkit.inventory.Recipe;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(DyeRecipe.class)
public abstract class DyeRecipeMixin implements RecipeBridge {

    // @formatter:off
    @Shadow @Final private ItemStackTemplate result;
    // @formatter:on

    @Override
    public Recipe bridge$toBukkitRecipe(NamespacedKey id) {
        CraftComplexRecipe recipe = new CraftComplexRecipe(id, CraftItemStack.asCraftMirror(this.result.create()), (DyeRecipe) (Object) this);
        recipe.setGroup(((DyeRecipe) (Object) this).group());
        recipe.setCategory(CraftRecipe.getCategory(((DyeRecipe) (Object) this).category()));
        return recipe;
    }
}
