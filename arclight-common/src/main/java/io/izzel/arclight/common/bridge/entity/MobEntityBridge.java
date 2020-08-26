package io.izzel.arclight.common.bridge.entity;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.util.ResourceLocation;
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

    interface Hack {

        ResourceLocation getLootTable();
    }
}
