package io.izzel.arclight.common.mixin.core.world.level.storage.loot.functions;

import io.izzel.arclight.common.bridge.core.world.storage.loot.LootContextBridge;
import io.izzel.arclight.common.mod.ArclightConstants;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootingEnchantFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(LootingEnchantFunction.class)
public abstract class LootingEnchantBonusMixin {

    // @formatter:off
    @Shadow @Final private NumberProvider value;
    @Shadow @Final private int limit;
    @Shadow protected abstract boolean hasLimit();
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public ItemStack run(ItemStack stack, LootContext context) {
        Entity entity = context.getParamOrNull(LootContextParams.KILLER_ENTITY);
        if (entity instanceof LivingEntity) {
            int i = ((LootContextBridge) context).bridge$forge$getLootingModifier(entity);
            if (context.hasParam(ArclightConstants.LOOTING_MOD)) {
                i = context.getParamOrNull(ArclightConstants.LOOTING_MOD);
            }
            if (i <= 0) {
                return stack;
            }

            float f = (float) i * this.value.getFloat(context);
            stack.grow(Math.round(f));
            if (this.hasLimit() && stack.getCount() > this.limit) {
                stack.setCount(this.limit);
            }
        }

        return stack;
    }
}
