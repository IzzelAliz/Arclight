package io.izzel.arclight.common.bridge.core.entity;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTransformEvent;

public interface MobEntityBridge extends LivingEntityBridge {

    void bridge$pushGoalTargetReason(EntityTargetEvent.TargetReason reason, boolean fireEvent);

    void bridge$pushTransformReason(EntityTransformEvent.TransformReason transformReason);

    boolean bridge$setGoalTarget(LivingEntity livingEntity, EntityTargetEvent.TargetReason reason, boolean fireEvent);

    boolean bridge$lastGoalTargetResult();

    ResourceLocation bridge$getLootTable();

    boolean bridge$isPersistenceRequired();

    void bridge$setPersistenceRequired(boolean value);

    void bridge$setAware(boolean aware);

    void bridge$captureItemDrop(ItemEntity itemEntity);
}
