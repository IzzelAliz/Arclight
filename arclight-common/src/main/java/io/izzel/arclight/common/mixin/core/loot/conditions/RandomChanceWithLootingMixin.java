package io.izzel.arclight.common.mixin.core.loot.conditions;

import io.izzel.arclight.common.mod.ArclightConstants;
import net.minecraft.loot.LootContext;
import net.minecraft.loot.conditions.RandomChanceWithLooting;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(RandomChanceWithLooting.class)
public class RandomChanceWithLootingMixin {

    @Shadow @Final private float chance;
    @Shadow @Final private float lootingMultiplier;

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public boolean test(LootContext context) {
        int i = context.getLootingModifier();

        if (context.has(ArclightConstants.LOOTING_MOD)) {
            i = context.get(ArclightConstants.LOOTING_MOD);
        }
        return context.getRandom().nextFloat() < this.chance + (float) i * this.lootingMultiplier;
    }
}
