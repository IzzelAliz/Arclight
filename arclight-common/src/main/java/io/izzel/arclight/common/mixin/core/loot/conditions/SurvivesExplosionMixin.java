package io.izzel.arclight.common.mixin.core.loot.conditions;

import net.minecraft.loot.LootContext;
import net.minecraft.loot.LootParameters;
import net.minecraft.loot.conditions.SurvivesExplosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.Random;

@Mixin(SurvivesExplosion.class)
public class SurvivesExplosionMixin {

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public boolean test(LootContext context) {
        Float f = context.get(LootParameters.EXPLOSION_RADIUS);
        if (f != null) {
            Random random = context.getRandom();
            float f1 = 1.0F / f;
            return random.nextFloat() < f1;
        } else {
            return true;
        }
    }
}
