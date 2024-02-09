package io.izzel.arclight.forge.mixin.core.world.item.crafting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.izzel.arclight.common.bridge.core.world.item.crafting.RecipeManagerBridge;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraftforge.common.crafting.conditions.ICondition;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(RecipeManager.class)
public abstract class RecipeManagerMixin_Forge implements RecipeManagerBridge {

    // @formatter:off
    @Shadow(remap = false) @Final private ICondition.IContext context;
    @Shadow protected static RecipeHolder<?> fromJson(ResourceLocation p_44046_, JsonObject p_44047_) { return null; }
    // @formatter:on

    @Override
    public RecipeHolder<?> bridge$platform$loadRecipe(ResourceLocation key, JsonElement element) {
        if (element.isJsonObject() && !net.minecraftforge.common.ForgeHooks.readAndTestCondition(this.context, element.getAsJsonObject())) {
            return null;
        }
        return fromJson(key, GsonHelper.convertToJsonObject(element, "top element"));
    }
}
