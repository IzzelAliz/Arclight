package io.izzel.arclight.common.mixin.core.world.entity.animal;

import io.izzel.arclight.common.bridge.core.world.entity.animal.PigBridge;
import io.izzel.arclight.common.bridge.core.world.level.WorldBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.pig.Pig;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Pig.class)
public abstract class PigMixin extends AnimalMixin implements PigBridge {

    @Decorate(method = "thunderHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/pig/Pig;convertTo(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/ConversionParams;Lnet/minecraft/world/entity/ConversionParams$AfterConversion;)Lnet/minecraft/world/entity/Mob;"))
    private Mob arclight$pigZap(Pig pig, EntityType<? extends Mob> entityType, ConversionParams conversionParams, ConversionParams.AfterConversion afterConversion, ServerLevel world, LightningBolt lightningBolt) throws Throwable {
        LightningBolt previous = this.arclight$lightningBolt;
        this.arclight$lightningBolt = lightningBolt;
        try {
            ((WorldBridge) pig.level()).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.LIGHTNING);
            this.bridge$pushTransformReason(EntityTransformEvent.TransformReason.LIGHTNING);
            return (Mob) DecorationOps.callsite().invoke(pig, entityType, conversionParams, afterConversion);
        } finally {
            this.arclight$lightningBolt = previous;
        }
    }

    private transient LightningBolt arclight$lightningBolt;

    @Override
    public LightningBolt bridge$getPigZapLightning() {
        return this.arclight$lightningBolt;
    }

    @Override
    public void bridge$clearPigZapLightning() {
        this.arclight$lightningBolt = null;
    }
}
