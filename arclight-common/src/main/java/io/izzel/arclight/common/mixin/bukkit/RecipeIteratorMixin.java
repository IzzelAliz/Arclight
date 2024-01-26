package io.izzel.arclight.common.mixin.bukkit;

import io.izzel.arclight.common.bridge.core.world.item.crafting.RecipeHolderBridge;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
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
    @Shadow @Final private Iterator<Map.Entry<RecipeType<?>, Object2ObjectLinkedOpenHashMap<ResourceLocation, RecipeHolder<?>>>> recipes;
    @Shadow private Iterator<RecipeHolder<?>> current;
    // @formatter:on

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
        var recipe = current.next();
        try {
            return ((RecipeHolderBridge) (Object) recipe).bridge$toBukkitRecipe();
        } catch (Throwable e) {
            throw new RuntimeException("Error converting recipe " + recipe.id(), e);
        }
    }
}
