package io.izzel.arclight.common.mixin.core.world.item;

import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(ProjectileWeaponItem.class)
public class ProjectileWeaponItemMixin {

    @Decorate(method = "shoot", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean arclight$shootBow(ServerLevel instance, Entity entity, ServerLevel serverLevel, LivingEntity livingEntity, InteractionHand interactionHand, ItemStack itemStack,
                                      List<ItemStack> list, float f, @Local(ordinal = -1) ItemStack itemstack1) throws Throwable {
        var event = CraftEventFactory.callEntityShootBowEvent(livingEntity, itemStack, itemstack1, entity, interactionHand, f, true);
        if (event.isCancelled()) {
            event.getProjectile().remove();
            return (boolean) DecorationOps.cancel().invoke();
        }

        if (event.getProjectile() == entity.bridge$getBukkitEntity()) {
            if (!(boolean) DecorationOps.callsite().invoke(instance, entity)) {
                if (livingEntity instanceof net.minecraft.server.level.ServerPlayer) {
                    ((ServerPlayerEntityBridge) livingEntity).bridge$getBukkitEntity().updateInventory();
                }
                return false;
            }
        }
        return true;
    }
}
