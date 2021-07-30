package io.izzel.arclight.common.mixin.core.world.entity.ai.brain;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.entity.Item;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(BehaviorUtils.class)
public class BrainUtilMixin {

    @Inject(method = "throwItem", cancellable = true, at = @At("HEAD"))
    private static void arclight$noEmptyLoot(LivingEntity entity, ItemStack stack, Vec3 offset, CallbackInfo ci) {
        if (stack.isEmpty()) ci.cancel();
    }

    @Inject(method = "throwItem", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private static void arclight$entityDropItem(LivingEntity entity, ItemStack stack, Vec3 offset, CallbackInfo ci, double d, ItemEntity itemEntity) {
        EntityDropItemEvent event = new EntityDropItemEvent(((EntityBridge) entity).bridge$getBukkitEntity(), (Item) ((EntityBridge) itemEntity).bridge$getBukkitEntity());
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
    }
}
