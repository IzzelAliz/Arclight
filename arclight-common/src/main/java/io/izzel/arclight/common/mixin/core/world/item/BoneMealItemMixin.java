package io.izzel.arclight.common.mixin.core.world.item;

import io.izzel.arclight.common.mod.mixins.annotation.TransformAccess;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Decorate;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gameevent.GameEvent;
import org.spongepowered.asm.mixin.injection.At;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BoneMealItem.class)
public class BoneMealItemMixin {

    @Decorate(method = "useOn(Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;causeUseVibration(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/Holder$Reference;)V"), require = 2, expect = 2, allow = 2)
    private void arclight$nullPlayerVibration(ItemStack stack, Entity causer, Holder.Reference<GameEvent> event) throws Throwable {
        if (causer != null) {
            DecorationOps.callsite().invoke(stack, causer, event);
        }
    }

    @TransformAccess(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC)
    private static InteractionResult applyBonemeal(UseOnContext itemactioncontext) {
        return Items.BONE_MEAL.useOn(itemactioncontext);
    }
}
