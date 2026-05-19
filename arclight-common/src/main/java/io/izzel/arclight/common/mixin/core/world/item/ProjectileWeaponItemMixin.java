package io.izzel.arclight.common.mixin.core.world.item;

import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;
import java.util.function.Consumer;

@Mixin(ProjectileWeaponItem.class)
public class ProjectileWeaponItemMixin {

    @Decorate(method = "shoot", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/Projectile;spawnProjectile(Lnet/minecraft/world/entity/projectile/Projectile;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Ljava/util/function/Consumer;)Lnet/minecraft/world/entity/projectile/Projectile;"))
    private Projectile arclight$shootBow(Projectile projectile, ServerLevel instance, ItemStack projectileStack, Consumer<Projectile> consumer, ServerLevel serverLevel, LivingEntity livingEntity, InteractionHand interactionHand, ItemStack itemStack,
                                         List<ItemStack> list, float f, float g, boolean bl, LivingEntity target) throws Throwable {
        var event = CraftEventFactory.callEntityShootBowEvent(livingEntity, itemStack, projectileStack, projectile, interactionHand, f, true);
        if (event.isCancelled()) {
            event.getProjectile().remove();
            return (Projectile) DecorationOps.cancel().invoke();
        }

        if (event.getProjectile() == projectile.bridge$getBukkitEntity()) {
            return (Projectile) DecorationOps.callsite().invoke(projectile, instance, projectileStack, consumer);
        }
        return projectile;
    }
}
