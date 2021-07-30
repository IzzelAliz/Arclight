package io.izzel.arclight.common.mixin.core.world.item.crafting;

import io.izzel.arclight.common.bridge.core.item.crafting.IRecipeBridge;
import io.izzel.arclight.common.mod.util.ArclightSpecialRecipe;
import net.minecraft.world.item.ItemStack;
import org.bukkit.inventory.Recipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(net.minecraft.world.item.crafting.Recipe.class)
public interface RecipeMixin extends IRecipeBridge {

    // @formatter:off
    @Shadow ItemStack getResultItem();
    // @formatter:on

    default Recipe toBukkitRecipe() {
        return bridge$toBukkitRecipe();
    }

    @Override
    default Recipe bridge$toBukkitRecipe() {
        return new ArclightSpecialRecipe((net.minecraft.world.item.crafting.Recipe<?>) this);
    }
}
