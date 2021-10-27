package io.izzel.arclight.common.mixin.bukkit;

import io.izzel.arclight.common.bridge.core.item.crafting.IRecipeBridge;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeType;
import org.bukkit.craftbukkit.v.inventory.RecipeIterator;
import org.bukkit.inventory.Recipe;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Iterator;
import java.util.Map;

@Mixin(value = RecipeIterator.class, remap = false)
public class RecipeIteratorMixin {

    // @formatter:off
    @Shadow private Iterator<net.minecraft.world.item.crafting.Recipe<?>> current;
    @Shadow @Final private Iterator<Map.Entry<RecipeType<?>, Map<ResourceLocation, net.minecraft.world.item.crafting.Recipe<?>>>> recipes;
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public boolean hasNext() {
        if (current != null && current.hasNext()) {
            return true;
        }
        if (recipes.hasNext()) {
            current = recipes.next().getValue().values().iterator();
            return hasNext();
        }
        return false;
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public Recipe next() {
        if (current == null || !current.hasNext()) {
            current = recipes.next().getValue().values().iterator();
            return next();
        }
        net.minecraft.world.item.crafting.Recipe<?> recipe = current.next();
        try {
            return ((IRecipeBridge) recipe).bridge$toBukkitRecipe();
        } catch (Throwable e) {
            throw new RuntimeException("Error converting recipe " + recipe.getId(), e);
        }
    }
}
