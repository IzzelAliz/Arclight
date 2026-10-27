package io.izzel.arclight.common.mixin.core.world.item.enchantment;

import io.izzel.arclight.common.mod.mixins.annotation.TransformAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;

import java.util.function.Consumer;

@Mixin(EnchantmentHelper.class)
public class EnchantmentHelperMixin {

    @TransformAccess(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC)
    private static ItemEnchantments updateEnchantments(ItemStack stack, Consumer<ItemEnchantments.Mutable> consumer, boolean createComponentIfMissing) {
        if (!createComponentIfMissing) {
            return EnchantmentHelper.updateEnchantments(stack, consumer);
        }
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(EnchantmentHelper.getEnchantmentsForCrafting(stack));
        consumer.accept(mutable);
        ItemEnchantments result = mutable.toImmutable();
        EnchantmentHelper.setEnchantments(stack, result);
        return result;
    }
}
