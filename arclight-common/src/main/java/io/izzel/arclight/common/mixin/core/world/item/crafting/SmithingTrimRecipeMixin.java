package io.izzel.arclight.common.mixin.core.world.item.crafting;



import io.izzel.arclight.common.bridge.core.world.item.crafting.RecipeBridge;

import io.izzel.arclight.common.mod.util.ArclightSpecialRecipe;

import net.minecraft.world.item.crafting.SmithingTrimRecipe;

import org.bukkit.NamespacedKey;

import org.bukkit.inventory.Recipe;

import org.spongepowered.asm.mixin.Mixin;



@Mixin(SmithingTrimRecipe.class)

public class SmithingTrimRecipeMixin implements RecipeBridge {



    @Override

    public Recipe bridge$toBukkitRecipe(NamespacedKey id) {

        return new ArclightSpecialRecipe(id, (SmithingTrimRecipe) (Object) this);

    }

}

