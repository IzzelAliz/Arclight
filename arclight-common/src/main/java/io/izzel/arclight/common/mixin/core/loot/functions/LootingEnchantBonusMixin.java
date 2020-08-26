package io.izzel.arclight.common.mixin.core.loot.functions;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootContext;
import net.minecraft.loot.LootParameters;
import net.minecraft.loot.RandomValueRange;
import net.minecraft.loot.functions.LootingEnchantBonus;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import io.izzel.arclight.common.mod.ArclightConstants;

@Mixin(LootingEnchantBonus.class)
public abstract class LootingEnchantBonusMixin {

    // @formatter:off
    @Shadow @Final private RandomValueRange count;
    @Shadow @Final private int limit;
    @Shadow protected abstract boolean func_215917_b();
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public ItemStack doApply(ItemStack stack, LootContext context) {
        Entity entity = context.get(LootParameters.KILLER_ENTITY);
        if (entity instanceof LivingEntity) {
            int i = context.getLootingModifier();
            if (context.has(ArclightConstants.LOOTING_MOD)) {
                i = context.get(ArclightConstants.LOOTING_MOD);
            }
            if (i <= 0) {
                return stack;
            }

            float f = (float) i * this.count.generateFloat(context.getRandom());
            stack.grow(Math.round(f));
            if (this.func_215917_b() && stack.getCount() > this.limit) {
                stack.setCount(this.limit);
            }
        }

        return stack;
    }
}
