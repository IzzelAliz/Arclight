package io.izzel.arclight.common.mixin.core.world.level.storage.loot.predicates;

import io.izzel.arclight.common.mod.ArclightConstants;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceWithLootingCondition;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(LootItemRandomChanceWithLootingCondition.class)
public class RandomChanceWithLootingMixin {

    @Shadow @Final private float percent;
    @Shadow @Final private float lootingMultiplier;

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public boolean test(LootContext context) {
        int i = context.getLootingModifier();

        if (context.hasParam(ArclightConstants.LOOTING_MOD)) {
            i = context.getParamOrNull(ArclightConstants.LOOTING_MOD);
        }
        return context.getRandom().nextFloat() < this.percent + (float) i * this.lootingMultiplier;
    }
}
