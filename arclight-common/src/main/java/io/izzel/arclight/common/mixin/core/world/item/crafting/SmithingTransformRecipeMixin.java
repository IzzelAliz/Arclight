package io.izzel.arclight.common.mixin.core.world.item.crafting;

import io.izzel.arclight.common.bridge.core.world.item.crafting.RecipeBridge;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import net.minecraft.world.item.ItemStackTemplate;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.inventory.CraftRecipe;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.inventory.CraftSmithingTransformRecipe;
import org.bukkit.inventory.Recipe;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Optional;

@Mixin(SmithingTransformRecipe.class)
public class SmithingTransformRecipeMixin implements RecipeBridge {

    // @formatter:off
    @Shadow @Final ItemStackTemplate result;
    @Shadow @Final Optional<Ingredient> template;
    @Shadow @Final Ingredient base;
    @Shadow @Final Optional<Ingredient> addition;
    // @formatter:on

    @Override
    public Recipe bridge$toBukkitRecipe(NamespacedKey id) {
        return new CraftSmithingTransformRecipe(id, CraftItemStack.asCraftMirror(this.result),
            CraftRecipe.toBukkit(this.template), CraftRecipe.toBukkit(this.base), CraftRecipe.toBukkit(this.addition));
    }
}
