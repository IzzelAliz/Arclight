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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.bukkit.Bukkit;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(MushroomCow.class)
public abstract class MushroomCowMixin_Forge {

    // Keep Forge's canLivingConvert gate and onLivingConvert callback intact.
    @WrapOperation(method = "shearInternal(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/sounds/SoundSource;Lnet/minecraft/world/item/ItemStack;)Ljava/util/List;", remap = false,
        at = @At(value = "INVOKE", remap = true, target = "Lnet/minecraft/world/entity/animal/cow/MushroomCow;convertTo(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/ConversionParams;Lnet/minecraft/world/entity/ConversionParams$AfterConversion;)Lnet/minecraft/world/entity/Mob;"),
        require = 1, expect = 1, allow = 1)
    private Mob arclight$animalTransform(MushroomCow cow, EntityType<?> type, ConversionParams params,
                                        ConversionParams.AfterConversion<?> afterConversion, Operation<Mob> original,
                                        @Cancellable CallbackInfoReturnable<List<ItemStack>> cir) {
        MobBridge mob = (MobBridge) cow;
        WorldBridge world = (WorldBridge) cow.level();
        EntityTransformEvent.TransformReason previousTransform = mob.bridge$getTransformReason();
        CreatureSpawnEvent.SpawnReason previousSpawn = world.bridge$getAddEntityReason();
        mob.bridge$pushTransformReason(EntityTransformEvent.TransformReason.SHEARED);
        world.bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.SHEARED);
        try {
            Mob converted = original.call(cow, type, params, afterConversion);
            if (converted == null) {
                // The conversion callback may have collected loot before Bukkit vetoed insertion.
                cir.setReturnValue(List.of());
            }
            return converted;
        } finally {
            mob.bridge$pushTransformReason(previousTransform);
            world.bridge$pushAddEntityReason(previousSpawn);
        }
    }

    @Decorate(method = "shear(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/sounds/SoundSource;Lnet/minecraft/world/item/ItemStack;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"),
        require = 1, expect = 1, allow = 1)
    private boolean arclight$dropShearedItem(Level level, Entity itemEntity) throws Throwable {
        EntityDropItemEvent event = new EntityDropItemEvent(((EntityBridge) this).bridge$getBukkitEntity(),
            (org.bukkit.entity.Item) ((EntityBridge) itemEntity).bridge$getBukkitEntity());
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }
        return (boolean) DecorationOps.callsite().invoke(level, itemEntity);
    }
}
