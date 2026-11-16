package io.izzel.arclight.forge.mixin.core.world.entity.animal;

import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Cancellable;
import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.world.entity.MobBridge;
import io.izzel.arclight.common.bridge.core.world.level.WorldBridge;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import net.minecraft.server.level.ServerLevel;
import org.bukkit.Bukkit;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MushroomCow.class)
public abstract class MushroomCowMixin_Forge {

    // Keep Forge's canLivingConvert gate and onLivingConvert callback intact.
    @WrapOperation(method = "shear(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/sounds/SoundSource;Lnet/minecraft/world/item/ItemStack;)V",
        at = @At(value = "INVOKE", remap = true, target = "Lnet/minecraft/world/entity/animal/cow/MushroomCow;convertTo(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/ConversionParams;Lnet/minecraft/world/entity/ConversionParams$AfterConversion;)Lnet/minecraft/world/entity/Mob;"),
        require = 1, expect = 1, allow = 1)
    private Mob arclight$animalTransform(MushroomCow cow, EntityType<?> type, ConversionParams params,
                                        ConversionParams.AfterConversion<?> afterConversion, Operation<Mob> original,
                                        @Cancellable CallbackInfo ci) {
        MobBridge mob = (MobBridge) cow;
        WorldBridge world = (WorldBridge) cow.level();
        EntityTransformEvent.TransformReason previousTransform = mob.bridge$getTransformReason();
        CreatureSpawnEvent.SpawnReason previousSpawn = world.bridge$getAddEntityReason();
        mob.bridge$pushTransformReason(EntityTransformEvent.TransformReason.SHEARED);
        world.bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.SHEARED);
        try {
            Mob converted = original.call(cow, type, params, afterConversion);
            if (converted == null) {
                ci.cancel();
            }
            return converted;
        } finally {
            mob.bridge$pushTransformReason(previousTransform);
            world.bridge$pushAddEntityReason(previousSpawn);
        }
    }

    @Decorate(method = "lambda$shear$2(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"),
        require = 1, expect = 1, allow = 1)
    private boolean arclight$dropShearedItem(ServerLevel level, Entity itemEntity) throws Throwable {
        EntityDropItemEvent event = new EntityDropItemEvent(((EntityBridge) this).bridge$getBukkitEntity(),
            (org.bukkit.entity.Item) ((EntityBridge) itemEntity).bridge$getBukkitEntity());
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }
        return (boolean) DecorationOps.callsite().invoke(level, itemEntity);
    }
}
