package io.izzel.arclight.common.mixin.core.world.item.crafting;

import io.izzel.arclight.common.bridge.core.world.item.crafting.RecipeBridge;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.ImbueRecipe;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.inventory.CraftComplexRecipe;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.inventory.CraftRecipe;
import org.bukkit.inventory.Recipe;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ImbueRecipe.class)
public abstract class ImbueRecipeMixin implements RecipeBridge {

    // @formatter:off
    @Shadow @Final private ItemStackTemplate result;
    // @formatter:on

    @Override
    public Recipe bridge$toBukkitRecipe(NamespacedKey id) {
        CraftComplexRecipe recipe = new CraftComplexRecipe(id, CraftItemStack.asCraftMirror(this.result.create()), (ImbueRecipe) (Object) this);
        recipe.setGroup(((ImbueRecipe) (Object) this).group());
        recipe.setCategory(CraftRecipe.getCategory(((ImbueRecipe) (Object) this).category()));
        return recipe;
    }
}
