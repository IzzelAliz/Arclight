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
public class SmithingTransformRecipeMixin implements RecipeBridge, io.izzel.arclight.common.bridge.core.world.item.crafting.SmithingTransformRecipeBridge {

    private boolean arclight$copyDataComponents = true;

    @Override
    public void bridge$setCopyDataComponents(boolean copy) {
        this.arclight$copyDataComponents = copy;
    }

    @org.spongepowered.asm.mixin.injection.Inject(method = "assemble(Lnet/minecraft/world/item/crafting/SmithingRecipeInput;)Lnet/minecraft/world/item/ItemStack;", at = @org.spongepowered.asm.mixin.injection.At("HEAD"), cancellable = true)
    private void arclight$copyComponents(net.minecraft.world.item.crafting.SmithingRecipeInput input, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<net.minecraft.world.item.ItemStack> cir) {
        if (!this.arclight$copyDataComponents) {
            cir.setReturnValue(this.result.create());
        }
    }

    // @formatter:off
    @Shadow @Final ItemStackTemplate result;
    @Shadow @Final Optional<Ingredient> template;
    @Shadow @Final Ingredient base;
    @Shadow @Final Optional<Ingredient> addition;
    // @formatter:on

    @Override
    public Recipe bridge$toBukkitRecipe(NamespacedKey id) {
        return new CraftSmithingTransformRecipe(id, CraftItemStack.asCraftMirror(this.result.create()),
            CraftRecipe.toChoice(this.template), CraftRecipe.toChoice(this.base), CraftRecipe.toChoice(this.addition), this.arclight$copyDataComponents);
    }
}
