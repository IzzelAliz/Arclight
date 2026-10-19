package io.izzel.arclight.common.mixin.core.world.item.crafting;

import io.izzel.arclight.common.bridge.core.world.item.crafting.IngredientBridge;
import io.izzel.arclight.common.bridge.core.world.item.crafting.RecipeBridge;
import io.izzel.arclight.common.mod.util.ArclightSpecialRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.inventory.CraftRecipe;
import org.bukkit.craftbukkit.inventory.CraftShapelessRecipe;
import org.bukkit.inventory.Recipe;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.entity.player.StackedItemContents;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Mixin(ShapelessRecipe.class)
public abstract class ShapelessRecipeMixin implements RecipeBridge {

    // @formatter:off
    @Shadow @Final ItemStackTemplate result;
    @Shadow @Final List<Ingredient> ingredients;
    // @formatter:on

    @WrapOperation(method = "matches(Lnet/minecraft/world/item/crafting/CraftingInput;Lnet/minecraft/world/level/Level;)Z",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/StackedItemContents;canCraft(Lnet/minecraft/world/item/crafting/Recipe;Lnet/minecraft/world/entity/player/StackedContents$Output;)Z"))
    private boolean arclight$exactMatches(StackedItemContents contents,
                                         net.minecraft.world.item.crafting.Recipe<?> recipe,
                                         StackedContents.Output<?> output, Operation<Boolean> original,
                                         @Local(argsOnly = true) CraftingInput input) {
        // Only replace the holder-only operation for component-sensitive choices.
        // All other paths and the supplied operation for non-exact recipes remain native.
        if (this.ingredients.stream().noneMatch(ingredient -> ((IngredientBridge) (Object) ingredient).bridge$isExact())) {
            return original.call(contents, recipe, output);
        }
        List<ItemStack> items = new ArrayList<>(input.ingredientCount());
        for (ItemStack item : input.items()) {
            if (!item.isEmpty()) {
                items.add(item);
            }
        }
        int[] assigned = new int[items.size()];
        Arrays.fill(assigned, -1);
        for (int ingredient = 0; ingredient < this.ingredients.size(); ingredient++) {
            if (!this.arclight$assignExact(ingredient, items, assigned, new boolean[items.size()])) {
                return false;
            }
        }
        return true;
    }

    @Unique
    private boolean arclight$assignExact(int ingredient, List<ItemStack> items, int[] assigned, boolean[] visited) {
        for (int slot = 0; slot < items.size(); slot++) {
            if (!visited[slot] && this.ingredients.get(ingredient).test(items.get(slot))) {
                visited[slot] = true;
                // Augment the existing matching: overlapping choices must not
                // greedily consume the only slot available to a later choice.
                if (assigned[slot] == -1 || this.arclight$assignExact(assigned[slot], items, assigned, visited)) {
                    assigned[slot] = ingredient;
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public Recipe bridge$toBukkitRecipe(NamespacedKey id) {
        if (this.result.create().isEmpty()) {
            return new ArclightSpecialRecipe(id, (ShapelessRecipe) (Object) this);
        }
        CraftItemStack result = CraftItemStack.asCraftMirror(this.result);
        CraftShapelessRecipe recipe = new CraftShapelessRecipe(id, result, (ShapelessRecipe) (Object) this);
        // These final getters are inherited from NormalCraftingRecipe, not shadow targets here.
        recipe.setGroup(((ShapelessRecipe) (Object) this).group());
        recipe.setCategory(CraftRecipe.getCategory(((ShapelessRecipe) (Object) this).category()));
        for (Ingredient list : this.ingredients) {
            recipe.addIngredient(CraftRecipe.toBukkit(list));
        }
        return recipe;
    }
}
