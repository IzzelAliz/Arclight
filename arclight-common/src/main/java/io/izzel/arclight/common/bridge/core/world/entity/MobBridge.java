package io.izzel.arclight.common.bridge.core.world.entity;

import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.jetbrains.annotations.Nullable;

public interface MobBridge extends LivingEntityBridge {

    void bridge$pushGoalTargetReason(EntityTargetEvent.TargetReason reason, boolean fireEvent);

    void bridge$pushTransformReason(EntityTransformEvent.TransformReason transformReason);

    EntityTransformEvent.TransformReason bridge$getTransformReason();

    boolean bridge$setGoalTarget(LivingEntity livingEntity, EntityTargetEvent.TargetReason reason, boolean fireEvent);

    boolean bridge$lastGoalTargetResult();

    boolean bridge$isPersistenceRequired();

    boolean bridge$deferConversionEquipment();

    void bridge$clearConversionEquipment();

    void bridge$clearDeferredConversionEquipment();

    void bridge$setPersistenceRequired(boolean value);

    void bridge$setAware(boolean aware);

    void bridge$captureItemDrop(ItemEntity itemEntity);

    default AgeableMob bridge$forge$onBabyEntitySpawn(Mob partner, @Nullable AgeableMob proposedChild) {
        return proposedChild;
    }

    boolean bridge$common$animalTameEvent(Player player);

    default void bridge$neoforge$setPreventConversionSpawns(boolean value) {
    }

    default boolean bridge$neoforge$preventConversionSpawns() {
        return false;
    }
}
