package io.izzel.arclight.common.mixin.core.world.item.crafting;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import io.izzel.arclight.common.bridge.core.world.item.crafting.RecipeBridge;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.TransmuteRecipe;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import java.util.List;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.inventory.CraftRecipe;
import org.bukkit.craftbukkit.inventory.CraftTransmuteRecipe;
import org.bukkit.inventory.Recipe;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TransmuteRecipe.class)
public class TransmuteRecipeMixin implements RecipeBridge {

    // @formatter:off
    @Shadow @Final private Ingredient input;
    @Shadow @Final private Ingredient material;
    @Shadow @Final private ItemStackTemplate result;
    @Shadow @Final private net.minecraft.advancements.criterion.MinMaxBounds.Ints materialCount;
    @Shadow @Final private boolean addMaterialCountToResult;
    @Shadow private int minMaterialCount() { throw new AssertionError(); }
    @Shadow private int maxMaterialCount() { throw new AssertionError(); }
    // @formatter:on

    @ModifyExpressionValue(method = "display()Ljava/util/List;", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/item/crafting/TransmuteRecipe;minMaterialCount()I"), require = 1)
    private int arclight$seedMaterialDisplay(int minimum, @Local(ordinal = 1) List<SlotDisplay> ingredients,
                                           @Local SlotDisplay materialDisplay) {
        // The native loop adds one material per option, including the first one.
        for (int count = 1; count < minimum; count++) {
            ingredients.add(materialDisplay);
        }
        return minimum;
    }

    @Override
    public Recipe bridge$toBukkitRecipe(NamespacedKey id) {
        CraftTransmuteRecipe recipe = new CraftTransmuteRecipe(id, this.result,
            CraftRecipe.toChoice(this.input), CraftRecipe.toChoice(this.material),
            this.materialCount, this.addMaterialCountToResult);
        recipe.setGroup(((TransmuteRecipe) (Object) this).group());
        recipe.setCategory(CraftRecipe.getCategory(((TransmuteRecipe) (Object) this).category()));
        return recipe;
    }
}
