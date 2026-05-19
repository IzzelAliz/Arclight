package io.izzel.arclight.common.mixin.core.world.item.crafting;

import io.izzel.arclight.common.bridge.core.world.item.crafting.RecipeBridge;
import io.izzel.arclight.common.mod.util.ArclightSpecialRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import net.minecraft.world.item.crafting.TransmuteResult;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v.inventory.CraftRecipe;
import org.bukkit.craftbukkit.v.inventory.CraftSmithingTransformRecipe;
import org.bukkit.inventory.Recipe;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Optional;

@Mixin(SmithingTransformRecipe.class)
public class SmithingTransformRecipeMixin implements RecipeBridge {

    // @formatter:off
    @Shadow @Final TransmuteResult result;
    @Shadow @Final Optional<Ingredient> template;
    @Shadow @Final Ingredient base;
    @Shadow @Final Optional<Ingredient> addition;
    // @formatter:on

    @Override
    public Recipe bridge$toBukkitRecipe(NamespacedKey id) {
        ItemStack resultStack = this.result.apply(ItemStack.EMPTY);
        if (resultStack.isEmpty() || this.template.isEmpty() || this.addition.isEmpty()) {
            return new ArclightSpecialRecipe(id, (SmithingTransformRecipe) (Object) this);
        }
        CraftItemStack result = CraftItemStack.asCraftMirror(resultStack);

        return new CraftSmithingTransformRecipe(id, result, CraftRecipe.toBukkit(this.template.get()), CraftRecipe.toBukkit(this.base), CraftRecipe.toBukkit(this.addition.get()));
    }
}
