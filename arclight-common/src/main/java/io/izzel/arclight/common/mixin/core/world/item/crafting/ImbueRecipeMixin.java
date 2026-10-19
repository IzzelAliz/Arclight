package io.izzel.arclight.common.mixin.core.world.item.crafting;

import io.izzel.arclight.common.bridge.core.world.item.crafting.RecipeBridge;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.ImbueRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.inventory.CraftImbueRecipe;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.inventory.CraftRecipe;
import org.bukkit.inventory.Recipe;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ImbueRecipe.class)
public class ImbueRecipeMixin implements RecipeBridge {

    // @formatter:off
    @Shadow @Final private Ingredient source;
    @Shadow @Final private Ingredient material;
    @Shadow @Final private ItemStackTemplate result;
    // @formatter:on

    @Override
    public Recipe bridge$toBukkitRecipe(NamespacedKey id) {
        return new CraftImbueRecipe(id, CraftItemStack.asCraftMirror(this.result),
            CraftRecipe.toBukkit(this.source), CraftRecipe.toBukkit(this.material));
    }
}
