package io.izzel.arclight.common.mixin.core.world.item;

import io.izzel.arclight.common.bridge.core.world.entity.LivingEntityBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import io.papermc.paper.event.entity.EntityAttemptSmashAttackEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.event.Event;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MaceItem.class)
public abstract class MaceItemSmashAttackEventMixin {

    // Only the hurtEnemy branch is event-controlled, just like Paper. Damage bonus,
    // item damage source and postHurtEnemy keep their native/loader calculations.
    @Decorate(method = "hurtEnemy(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/LivingEntity;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/MaceItem;canSmashAttack(Lnet/minecraft/world/entity/LivingEntity;)Z"),
        require = 1, allow = 1)
    private boolean arclight$attemptSmashAttack(LivingEntity attacker,
                                               @Local(ordinal = 0) ItemStack weapon,
                                               @Local(ordinal = 0) LivingEntity target) throws Throwable {
        boolean original = (boolean) DecorationOps.callsite().invoke(attacker);
        EntityAttemptSmashAttackEvent event = new EntityAttemptSmashAttackEvent(
            ((LivingEntityBridge) attacker).bridge$getBukkitEntity(),
            ((LivingEntityBridge) target).bridge$getBukkitEntity(), CraftItemStack.asBukkitCopy(weapon), original);
        event.callEvent();
        return event.getResult() == Event.Result.ALLOW || original && event.getResult() == Event.Result.DEFAULT;
    }
}
