package io.izzel.arclight.common.mixin.core.world.entity.monster;

import com.destroystokyo.paper.event.entity.WitchConsumePotionEvent;
import com.destroystokyo.paper.event.entity.WitchThrowPotionEvent;
import io.izzel.arclight.common.bridge.core.world.entity.LivingEntityBridge;
import io.izzel.arclight.common.mixin.core.world.entity.raid.RaiderMixin;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Consumer;

@Mixin(Witch.class)
public abstract class WitchMixin extends RaiderMixin {

    @Decorate(method = "aiStep()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/Witch;setItemSlot(Lnet/minecraft/world/entity/EquipmentSlot;Lnet/minecraft/world/item/ItemStack;)V", ordinal = 1), require = 1, allow = 1)
    private void arclight$readyPotion(Witch witch, EquipmentSlot slot, ItemStack potion) throws Throwable {
        // Cancellation readies EMPTY; Paper still runs the native duration, sound and speed tail.
        potion = CraftEventFactory.handleWitchReadyPotionEvent(witch, potion);
        DecorationOps.callsite().invoke(witch, slot, potion);
    }

    @Decorate(method = "aiStep()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;get(Lnet/minecraft/core/component/DataComponentType;)Ljava/lang/Object;"), require = 1, allow = 1)
    private Object arclight$consumePotion(ItemStack itemStack, DataComponentType<?> component) throws Throwable {
        Object contents = DecorationOps.callsite().invoke(itemStack, component);
        if (itemStack.is(Items.POTION)) {
            WitchConsumePotionEvent event = new WitchConsumePotionEvent((org.bukkit.entity.Witch) this.bridge$getBukkitEntity(), CraftItemStack.asCraftMirror(itemStack));
            // Suppress effects only: the hand is already empty, and DRINK / speed cleanup must run.
            contents = event.callEvent() ? CraftItemStack.unwrap(event.getPotion()).get(DataComponents.POTION_CONTENTS) : null;
        }
        return contents;
    }

    @Decorate(method = "performRangedAttack(Lnet/minecraft/world/entity/LivingEntity;F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/alchemy/PotionContents;createItemStack(Lnet/minecraft/world/item/Item;Lnet/minecraft/core/Holder;)Lnet/minecraft/world/item/ItemStack;"), require = 1, allow = 1)
    private ItemStack arclight$throwPotion(Item item, Holder<Potion> potion, @Local(ordinal = 0) LivingEntity target) throws Throwable {
        ItemStack itemStack = (ItemStack) DecorationOps.callsite().invoke(item, potion);
        WitchThrowPotionEvent event = new WitchThrowPotionEvent((org.bukkit.entity.Witch) this.bridge$getBukkitEntity(), ((LivingEntityBridge) target).bridge$getBukkitEntity(), CraftItemStack.asCraftMirror(itemStack));
        if (!event.callEvent()) {
            // Cancel the void attack method, not just projectile creation: no throw sound on veto.
            return (ItemStack) DecorationOps.cancel().invoke();
        }
        return CraftItemStack.asNMSCopy(event.getPotion());
    }

    @Decorate(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/alchemy/PotionContents;forEachEffect(Ljava/util/function/Consumer;F)V"))
    private void arclight$reason(PotionContents instance, Consumer<MobEffectInstance> consumer, float durationScale) throws Throwable {
        Consumer<MobEffectInstance> wrapped = effect -> {
            bridge$pushEffectCause(EntityPotionEffectEvent.Cause.ATTACK);
            consumer.accept(effect);
        };
        DecorationOps.callsite().invoke(instance, wrapped, durationScale);
    }
}
