package io.izzel.arclight.common.mixin.core.world.item.crafting;

import io.izzel.arclight.common.bridge.core.world.item.crafting.RecipeBridge;
import io.izzel.arclight.common.bridge.core.world.item.crafting.RecipeHolderBridge;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.bukkit.craftbukkit.v.util.CraftNamespacedKey;
import org.bukkit.inventory.Recipe;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(RecipeHolder.class)
public class RecipeHolderMixin<T extends net.minecraft.world.item.crafting.Recipe<?>> implements RecipeHolderBridge {

    @Shadow @Final private T value;
    @Shadow @Final private ResourceLocation id;

    @Override
    public Recipe bridge$toBukkitRecipe() {
        return toBukkitRecipe();
    }

    public Recipe toBukkitRecipe() {
        return ((RecipeBridge) this.value).bridge$toBukkitRecipe(CraftNamespacedKey.fromMinecraft(this.id));
    }
}
