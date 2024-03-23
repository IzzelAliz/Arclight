package io.izzel.arclight.neoforge.mixin.core.world.item.crafting;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.DynamicOps;
import io.izzel.arclight.common.bridge.core.world.item.crafting.RecipeManagerBridge;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.neoforge.common.conditions.ConditionalOps;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Optional;

@Mixin(RecipeManager.class)
public abstract class RecipeManagerMixin_NeoForge extends SimpleJsonResourceReloadListener implements RecipeManagerBridge {

    // @formatter:off
    @Shadow public static Optional<RecipeHolder<?>> fromJson(ResourceLocation arg, JsonObject jsonObject, DynamicOps<JsonElement> jsonElementOps) { return Optional.empty(); }
    // @formatter:on

    public RecipeManagerMixin_NeoForge(Gson gson, String string) {
        super(gson, string);
    }

    @Override
    public RecipeHolder<?> bridge$platform$loadRecipe(ResourceLocation key, JsonElement element) {
        ConditionalOps<JsonElement> ops = this.makeConditionalOps();
        return fromJson(key, GsonHelper.convertToJsonObject(element, "top element"), ops).orElse(null);
    }
}
