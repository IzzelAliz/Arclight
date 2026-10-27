package io.izzel.arclight.common.mixin.core.world.item;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ItemStack.class)
public abstract class ItemStackNaturalDamageEventMixin {

    @WrapMethod(method = "hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;)V")
    private void arclight$naturalEquipmentDamage(int amount, LivingEntity owner, EquipmentSlot slot, Operation<Void> original) {
        ItemStack stack = (ItemStack) (Object) this;
        if (owner instanceof ServerPlayer || ArclightCaptures.getItemDamageOwner(stack, owner) != null) {
            original.call(amount, owner, slot);
        } else {
            // Vanilla erases a non-player owner in the lower ServerPlayer overload.
            // Reuse its existing post-enchantment event consumer, with finally cleanup.
            ArclightCaptures.withItemDamage(stack, owner, false, () -> original.call(amount, owner, slot));
        }
    }
}
