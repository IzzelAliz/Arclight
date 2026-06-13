package io.izzel.arclight.neoforge.mixin.core.world.item.crafting;

import io.izzel.arclight.common.bridge.core.world.item.crafting.RecipeManagerBridge;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(RecipeManager.class)
public abstract class RecipeManagerMixin_NeoForge implements RecipeManagerBridge {
}
