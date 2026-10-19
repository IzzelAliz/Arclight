package io.izzel.arclight.common.mixin.core.world.entity.monster.hoglin;

import io.izzel.arclight.common.bridge.core.world.entity.MobBridge;
import io.izzel.arclight.common.bridge.core.world.level.WorldBridge;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Hoglin.class)
public abstract class HoglinMixin {

    @SuppressWarnings("unchecked")
    @Decorate(
        method = "finishConversion()V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/hoglin/Hoglin;convertTo(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/ConversionParams;Lnet/minecraft/world/entity/ConversionParams$AfterConversion;)Lnet/minecraft/world/entity/Mob;")
    )
    private <T extends Mob> T arclight$conversionReason(Hoglin hoglin, EntityType<T> entityType,
                                                         ConversionParams params,
                                                         ConversionParams.AfterConversion<T> afterConversion) throws Throwable {
        ((MobBridge) hoglin).bridge$pushTransformReason(EntityTransformEvent.TransformReason.PIGLIN_ZOMBIFIED);
        ((WorldBridge) hoglin.level()).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.PIGLIN_ZOMBIFIED);
        return (T) DecorationOps.callsite().invoke(hoglin, entityType, params, afterConversion);
    }
}
