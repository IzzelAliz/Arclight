package io.izzel.arclight.common.mixin.core.world.item.crafting;

import io.izzel.arclight.common.bridge.core.world.item.crafting.RecipeBridge;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.DyeRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.inventory.CraftDyeRecipe;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.inventory.CraftRecipe;
import org.bukkit.inventory.Recipe;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(DyeRecipe.class)
public class DyeRecipeMixin implements RecipeBridge {

    // @formatter:off
    @Shadow @Final private Ingredient target;
    @Shadow @Final private Ingredient dye;
    @Shadow @Final private ItemStackTemplate result;
    // @formatter:on

    @Override
    public Recipe bridge$toBukkitRecipe(NamespacedKey id) {
        return new CraftDyeRecipe(id, CraftItemStack.asCraftMirror(this.result),
            CraftRecipe.toBukkit(this.target), CraftRecipe.toBukkit(this.dye));
    }
}
