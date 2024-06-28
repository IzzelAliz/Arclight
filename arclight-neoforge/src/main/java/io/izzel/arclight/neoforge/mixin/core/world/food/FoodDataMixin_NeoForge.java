package io.izzel.arclight.neoforge.mixin.core.world.food;

import io.izzel.arclight.common.bridge.core.util.FoodStatsBridge;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(FoodData.class)
public abstract class FoodDataMixin_NeoForge implements FoodStatsBridge {

    // @formatter:off
    @Shadow public int foodLevel;
    @Shadow public abstract void eat(int i, float f);
    // @formatter:on
}
