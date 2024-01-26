package io.izzel.arclight.forge.mixin.core.world.item.crafting;

import com.google.gson.JsonElement;
import io.izzel.arclight.common.bridge.core.world.item.crafting.RecipeManagerBridge;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraftforge.common.crafting.conditions.ICondition;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(RecipeManager.class)
public abstract class RecipeManagerMixin_Forge implements RecipeManagerBridge {

    @Shadow(remap = false) @Final private ICondition.IContext context;

    @Override
    public boolean bridge$forge$conditionNotMet(JsonElement element) {
        return element.isJsonObject() && !net.minecraftforge.common.ForgeHooks.readAndTestCondition(this.context, element.getAsJsonObject());
    }
}
