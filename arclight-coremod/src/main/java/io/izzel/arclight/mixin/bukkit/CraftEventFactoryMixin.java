package io.izzel.arclight.mixin.bukkit;

import com.google.common.base.Function;
import io.izzel.arclight.mod.util.ArclightCaptures;
import net.minecraft.entity.Entity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.EntityDamageEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(value = CraftEventFactory.class, remap = false)
public class CraftEventFactoryMixin {

    @Shadow public static Entity entityDamage;
    @Shadow public static Block blockDamage;

    @Inject(method = "handleEntityDamageEvent(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/DamageSource;Ljava/util/Map;Ljava/util/Map;Z)Lorg/bukkit/event/entity/EntityDamageEvent;", at = @At("HEAD"))
    private static void arclight$captureSource(Entity entity, DamageSource source, Map<EntityDamageEvent.DamageModifier, Double> modifiers, Map<EntityDamageEvent.DamageModifier, Function<? super Double, Double>> modifierFunctions, boolean cancelled, CallbackInfoReturnable<EntityDamageEvent> cir) {
        Entity damageEventEntity = ArclightCaptures.getDamageEventEntity();
        BlockPos damageEventBlock = ArclightCaptures.getDamageEventBlock();
        if (damageEventEntity != null && entityDamage == null) {
            if (source.damageType.equals(DamageSource.LIGHTNING_BOLT.damageType)) {
                entityDamage = entity;
            }
        }
        if (damageEventBlock != null && blockDamage == null) {
            if (source.damageType.equals(DamageSource.CACTUS.damageType)
                || source.damageType.equals(DamageSource.SWEET_BERRY_BUSH.damageType)
                || source.damageType.equals(DamageSource.HOT_FLOOR.damageType)) {
                blockDamage = CraftBlock.at(entity.getEntityWorld(), damageEventBlock);
            }
        }
    }
}
