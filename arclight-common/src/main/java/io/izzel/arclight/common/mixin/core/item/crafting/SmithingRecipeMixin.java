package io.izzel.arclight.common.mixin.core.item.crafting;

import io.izzel.arclight.common.bridge.item.crafting.IRecipeBridge;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.SmithingRecipe;
import net.minecraft.util.ResourceLocation;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v.inventory.CraftRecipe;
import org.bukkit.craftbukkit.v.inventory.CraftSmithingRecipe;
import org.bukkit.craftbukkit.v.util.CraftNamespacedKey;
import org.bukkit.inventory.Recipe;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(SmithingRecipe.class)
public class SmithingRecipeMixin implements IRecipeBridge {

    // @formatter:off
    @Shadow @Final private ItemStack result;
    @Shadow @Final private ResourceLocation recipeId;
    @Shadow @Final private Ingredient base;
    @Shadow @Final private Ingredient addition;
    // @formatter:on

    @Override
    public Recipe bridge$toBukkitRecipe() {
        CraftItemStack result = CraftItemStack.asCraftMirror(this.result);
        return new CraftSmithingRecipe(CraftNamespacedKey.fromMinecraft(this.recipeId), result, CraftRecipe.toBukkit(this.base), CraftRecipe.toBukkit(this.addition));
    }
}
