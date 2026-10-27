package io.izzel.arclight.common.mixin.core.world.entity.npc;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import io.izzel.arclight.common.mod.util.VillagerZapConversion;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.villager.Villager;
import org.slf4j.Logger;
import org.spigotmc.SpigotConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Villager.class)
public abstract class VillagerZapEventMixin {

    @WrapOperation(method = "thunderHit(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LightningBolt;)V",
        at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;info(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V", remap = false))
    private void arclight$deferZapLog(Logger logger, String message, Object villager, Object lightning,
            Operation<Void> original, @Share("arclightVillagerZapLog") LocalRef<Runnable> log) {
        // Paper logs only once the zap event has accepted the actual replacement.
        log.set(() -> {
            if (SpigotConfig.logVillagerDeaths) {
                original.call(logger, message, villager, lightning);
            }
        });
    }

    @WrapOperation(method = "thunderHit(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LightningBolt;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/npc/villager/Villager;convertTo(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/ConversionParams;Lnet/minecraft/world/entity/ConversionParams$AfterConversion;)Lnet/minecraft/world/entity/Mob;"))
    private <T extends Mob> T arclight$villagerZap(Villager villager, EntityType<T> type, ConversionParams params,
            ConversionParams.AfterConversion<T> afterConversion, Operation<T> original,
            ServerLevel level, LightningBolt lightning,
            @Share("arclightVillagerZapLog") LocalRef<Runnable> log) {
        return original.call(villager, type, params,
            new VillagerZapConversion<>(villager, lightning, afterConversion, log.get()));
    }
}
