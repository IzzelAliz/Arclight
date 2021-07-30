package io.izzel.arclight.common.mixin.bukkit;

import com.google.common.base.Function;
import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.mod.ArclightMod;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;

@Mixin(value = CraftEventFactory.class, remap = false)
public class CraftEventFactoryMixin {

    @Shadow public static Entity entityDamage;
    @Shadow public static Block blockDamage;

    @Inject(method = "handleEntityDamageEvent(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;Ljava/util/Map;Ljava/util/Map;Z)Lorg/bukkit/event/entity/EntityDamageEvent;", at = @At("HEAD"))
    private static void arclight$captureSource(Entity entity, DamageSource source, Map<EntityDamageEvent.DamageModifier, Double> modifiers, Map<EntityDamageEvent.DamageModifier, Function<? super Double, Double>> modifierFunctions, boolean cancelled, CallbackInfoReturnable<EntityDamageEvent> cir) {
        Entity damageEventEntity = ArclightCaptures.getDamageEventEntity();
        BlockPos damageEventBlock = ArclightCaptures.getDamageEventBlock();
        if (damageEventEntity != null && entityDamage == null) {
            if (source.msgId.equals(DamageSource.LIGHTNING_BOLT.msgId)) {
                entityDamage = damageEventEntity;
            }
        }
        if (damageEventBlock != null && blockDamage == null) {
            if (source.msgId.equals(DamageSource.CACTUS.msgId)
                || source.msgId.equals(DamageSource.SWEET_BERRY_BUSH.msgId)
                || source.msgId.equals(DamageSource.HOT_FLOOR.msgId)) {
                blockDamage = CraftBlock.at(entity.getCommandSenderWorld(), damageEventBlock);
            }
        }
    }

    @Inject(method = "handleEntityDamageEvent(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;Ljava/util/Map;Ljava/util/Map;Z)Lorg/bukkit/event/entity/EntityDamageEvent;", cancellable = true, at = @At(value = "NEW", target = "java/lang/IllegalStateException"))
    private static void arclight$unhandledDamage(Entity entity, DamageSource source, Map<EntityDamageEvent.DamageModifier, Double> modifiers, Map<EntityDamageEvent.DamageModifier, Function<? super Double, Double>> modifierFunctions, boolean cancelled, CallbackInfoReturnable<EntityDamageEvent> cir) {
        // todo blockDamage is lost
        EntityDamageEvent event;
        if (source.getEntity() != null) {
            ArclightMod.LOGGER.debug("Unhandled damage of {} by {} from {}", entity, source.getEntity(), source.msgId);
            event = new EntityDamageByEntityEvent(((EntityBridge) source.getEntity()).bridge$getBukkitEntity(), ((EntityBridge) entity).bridge$getBukkitEntity(), EntityDamageEvent.DamageCause.CUSTOM, modifiers, modifierFunctions);
        } else {
            ArclightMod.LOGGER.debug("Unhandled damage of {} from {}", entity, source.msgId);
            event = new EntityDamageEvent(((EntityBridge) entity).bridge$getBukkitEntity(), EntityDamageEvent.DamageCause.CUSTOM, modifiers, modifierFunctions);
        }
        event.setCancelled(cancelled);
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            ((EntityBridge) entity).bridge$getBukkitEntity().setLastDamageCause(event);
        }
        cir.setReturnValue(event);
    }
}
