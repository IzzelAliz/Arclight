package io.izzel.arclight.common.mixin.core.world.entity.animal.frog;

import io.izzel.arclight.common.bridge.core.server.level.ServerLevelBridge;
import io.izzel.arclight.common.mixin.core.world.entity.PathfinderMobMixin;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.frog.Tadpole;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Tadpole.class)
public abstract class TadpoleMixin extends PathfinderMobMixin {

    @Shadow protected abstract void setAge(int age);

    @Decorate(method = "ageUp()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/frog/Tadpole;convertTo(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/ConversionParams;Lnet/minecraft/world/entity/ConversionParams$AfterConversion;)Lnet/minecraft/world/entity/Mob;"))
    private Mob arclight$transform(Tadpole tadpole, EntityType<?> type, ConversionParams params,
                                  ConversionParams.AfterConversion<?> afterConversion) throws Throwable {
        this.bridge$pushTransformReason(EntityTransformEvent.TransformReason.METAMORPHOSIS);
        ((ServerLevelBridge) this.level()).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.METAMORPHOSIS);
        Mob result = (Mob) DecorationOps.callsite().invoke(tadpole, type, params, afterConversion);
        if (result == null) {
            this.setAge(0);
        }
        return result;
    }
}
