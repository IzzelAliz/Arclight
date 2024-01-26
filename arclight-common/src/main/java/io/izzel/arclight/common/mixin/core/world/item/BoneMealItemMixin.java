package io.izzel.arclight.common.mixin.core.world.item;

import io.izzel.arclight.common.mod.mixins.annotation.TransformAccess;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BoneMealItem.class)
public class BoneMealItemMixin {

    @TransformAccess(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC)
    private static InteractionResult applyBonemeal(UseOnContext itemactioncontext) {
        return Items.BONE_MEAL.useOn(itemactioncontext);
    }
}
