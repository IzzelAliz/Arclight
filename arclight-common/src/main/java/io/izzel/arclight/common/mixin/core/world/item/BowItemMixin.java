package io.izzel.arclight.common.mixin.core.world.item;

import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.bukkit.craftbukkit.v.entity.CraftEntity;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Consumer;

@Mixin(BowItem.class)
public abstract class BowItemMixin extends ItemMixin {

    // @formatter:off
    @Shadow public abstract int getUseDuration(ItemStack stack);
    @Shadow public static float getPowerForTime(int charge) { return 0; }
    // @formatter:on

    @Decorate(method = "releaseUsing", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Consumer;)V"))
    private <T extends LivingEntity> void arclight$entityShootBow(
        ItemStack instance, int i, T livingEntity, Consumer<T> consumer,
        ItemStack stack, Level worldIn, LivingEntity entityLiving, int timeLeft,
        @Local(ordinal = -1) ItemStack projectile, @Local(ordinal = -1) AbstractArrow abstractArrow,
        @Local(ordinal = -1) float power, @Local(ordinal = -1) boolean consumeItem,
        @Local(allocate = "projectileEntity") Entity projectileEntity
    ) throws Throwable {
        EntityShootBowEvent event = CraftEventFactory.callEntityShootBowEvent(entityLiving, stack, projectile, abstractArrow, entityLiving.getUsedItemHand(), power, !consumeItem);
        if (event.isCancelled()) {
            event.getProjectile().remove();
            DecorationOps.cancel().invoke();
            return;
        }
        consumeItem = !event.shouldConsumeItem();
        projectileEntity = ((CraftEntity) event.getProjectile()).getHandle();
        DecorationOps.blackhole().invoke(consumeItem, projectileEntity);
        DecorationOps.callsite().invoke(instance, i, livingEntity, consumer);
    }

    @Decorate(method = "releaseUsing", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean arclight$addProjectile(Level instance, Entity entity, ItemStack itemStack, Level level, LivingEntity livingEntity, int i,
                                           @Local(allocate = "projectileEntity") Entity projectileEntity) throws Throwable {
        if (entity == projectileEntity) {
            if (!(boolean) DecorationOps.callsite().invoke(instance, entity)) {
                if (livingEntity instanceof ServerPlayerEntityBridge) {
                    ((ServerPlayerEntityBridge) livingEntity).bridge$getBukkitEntity().updateInventory();
                }
                return false;
            }
            return true;
        } else {
            return false;
        }
    }
}
