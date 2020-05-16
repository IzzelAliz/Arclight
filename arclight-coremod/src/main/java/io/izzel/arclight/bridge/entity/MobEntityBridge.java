package io.izzel.arclight.bridge.entity;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ResourceLocation;
import org.bukkit.event.entity.EntityTargetEvent;

public interface MobEntityBridge extends LivingEntityBridge {

    void bridge$pushGoalTargetReason(EntityTargetEvent.TargetReason reason, boolean fireEvent);

    boolean bridge$setGoalTarget(LivingEntity livingEntity, EntityTargetEvent.TargetReason reason, boolean fireEvent);

    ResourceLocation bridge$getLootTable();

    boolean bridge$isPersistenceRequired();

    void bridge$setPersistenceRequired(boolean value);

    interface Hack {

        ResourceLocation getLootTable();
    }
}
