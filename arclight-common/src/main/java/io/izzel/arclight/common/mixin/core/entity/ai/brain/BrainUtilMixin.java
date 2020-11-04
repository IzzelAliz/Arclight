package io.izzel.arclight.common.mixin.core.entity.ai.brain;

import io.izzel.arclight.common.bridge.entity.EntityBridge;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.BrainUtil;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.vector.Vector3d;
import org.bukkit.Bukkit;
import org.bukkit.entity.Item;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(BrainUtil.class)
public class BrainUtilMixin {

    @Inject(method = "spawnItemNearEntity", cancellable = true, at = @At("HEAD"))
    private static void arclight$noEmptyLoot(LivingEntity entity, ItemStack stack, Vector3d offset, CallbackInfo ci) {
        if (stack.isEmpty()) ci.cancel();
    }

    @Inject(method = "spawnItemNearEntity", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;addEntity(Lnet/minecraft/entity/Entity;)Z"))
    private static void arclight$entityDropItem(LivingEntity entity, ItemStack stack, Vector3d offset, CallbackInfo ci, double d, ItemEntity itemEntity) {
        EntityDropItemEvent event = new EntityDropItemEvent(((EntityBridge) entity).bridge$getBukkitEntity(), (Item) ((EntityBridge) itemEntity).bridge$getBukkitEntity());
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
    }
}
