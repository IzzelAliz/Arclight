package io.izzel.arclight.common.mixin.core.world.item.crafting;

import io.izzel.arclight.common.bridge.core.world.item.crafting.RecipeBridge;
import io.izzel.arclight.common.bridge.core.world.item.crafting.SmithingTrimRecipeBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SmithingTrimRecipe;
import net.minecraft.world.item.equipment.trim.TrimPattern;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.inventory.CraftRecipe;
import org.bukkit.craftbukkit.inventory.CraftSmithingTrimRecipe;
import org.bukkit.craftbukkit.inventory.trim.CraftTrimPattern;
import org.bukkit.inventory.Recipe;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SmithingTrimRecipe.class)
public class SmithingTrimRecipeMixin implements RecipeBridge, SmithingTrimRecipeBridge {

    private boolean arclight$copyDataComponents = true;
    @org.spongepowered.asm.mixin.Unique
    private static org.apache.commons.lang3.tuple.Triple<ItemStack, ItemStack, Holder<TrimPattern>> arclight$trimWithoutComponents;

    // @formatter:off
    @Shadow @Final Ingredient template;
    @Shadow @Final Ingredient base;
    @Shadow @Final Ingredient addition;
    @Shadow @Final private Holder<TrimPattern> pattern;
    // @formatter:on

    @Override
    public void bridge$setCopyDataComponents(boolean copy) {
        this.arclight$copyDataComponents = copy;
    }

    @Decorate(method = "assemble(Lnet/minecraft/world/item/crafting/SmithingRecipeInput;)Lnet/minecraft/world/item/ItemStack;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/crafting/SmithingTrimRecipe;applyTrim(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/core/Holder;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack arclight$trimComponents(ItemStack baseItem, ItemStack materialItem, Holder<TrimPattern> pattern) throws Throwable {
        var previous = arclight$trimWithoutComponents;
        arclight$trimWithoutComponents = this.arclight$copyDataComponents ? null
            : org.apache.commons.lang3.tuple.Triple.of(baseItem, materialItem, pattern);
        try {
            return (ItemStack) DecorationOps.callsite().invoke(baseItem, materialItem, pattern);
        } finally {
            arclight$trimWithoutComponents = previous;
        }
    }

    @Decorate(method = "applyTrim(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/core/Holder;)Lnet/minecraft/world/item/ItemStack;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;copyWithCount(I)Lnet/minecraft/world/item/ItemStack;"))
    private static ItemStack arclight$copyComponents(ItemStack baseItem, int count, ItemStack nativeBaseItem, ItemStack materialItem, Holder<TrimPattern> pattern) throws Throwable {
        if (arclight$trimWithoutComponents != null && arclight$trimWithoutComponents.getLeft() == nativeBaseItem
            && arclight$trimWithoutComponents.getMiddle() == materialItem && arclight$trimWithoutComponents.getRight() == pattern) {
            return new ItemStack(baseItem.getItem(), 1);
        }
        return (ItemStack) DecorationOps.callsite().invoke(baseItem, count);
    }

    @Override
    public Recipe bridge$toBukkitRecipe(NamespacedKey id) {
        return new CraftSmithingTrimRecipe(id, CraftRecipe.toChoice(this.template), CraftRecipe.toChoice(this.base), CraftRecipe.toChoice(this.addition), CraftTrimPattern.minecraftHolderToBukkit(this.pattern), this.arclight$copyDataComponents);
    }
}
