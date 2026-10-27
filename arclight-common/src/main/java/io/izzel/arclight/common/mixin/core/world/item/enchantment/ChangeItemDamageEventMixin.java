package io.izzel.arclight.common.mixin.core.world.item.enchantment;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.effects.ChangeItemDamage;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Consumer;

@Mixin(ChangeItemDamage.class)
public abstract class ChangeItemDamageEventMixin {

    @WrapOperation(method = "apply(Lnet/minecraft/server/level/ServerLevel;ILnet/minecraft/world/item/enchantment/EnchantedItemInUse;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/server/level/ServerPlayer;Ljava/util/function/Consumer;)V"))
    private void arclight$enchantmentItemDamage(ItemStack stack, int amount, ServerLevel level, ServerPlayer player,
            Consumer<Item> onBreak, Operation<Void> original, ServerLevel effectLevel, int enchantmentLevel,
            EnchantedItemInUse item, Entity target, Vec3 position) {
        // The effect target need not be the owner of the enchanted item.
        LivingEntity owner = item.owner();
        if (owner == null || owner instanceof ServerPlayer || ArclightCaptures.getItemDamageOwner(stack, owner) != null) {
            original.call(stack, amount, level, player, onBreak);
        } else {
            // Both native loaders erase non-player owners to a null ServerPlayer here.
            ArclightCaptures.withItemDamage(stack, owner, false, () -> original.call(stack, amount, level, player, onBreak));
        }
    }
}
