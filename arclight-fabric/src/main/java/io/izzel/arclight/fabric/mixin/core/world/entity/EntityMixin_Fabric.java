package io.izzel.arclight.fabric.mixin.core.world.entity;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Entity.class)
public abstract class EntityMixin_Fabric implements EntityBridge {

    // @formatter:off
    @Shadow public abstract Level level();
    @Shadow public abstract boolean isRemoved();
    @Shadow @org.jetbrains.annotations.Nullable protected abstract PortalInfo findDimensionEntryPoint(ServerLevel serverLevel);
    @Shadow private float yRot;
    @Shadow private float xRot;
    @Shadow public abstract float getXRot();
    @Shadow public abstract void moveTo(double d, double e, double f, float g, float h);
    @Shadow public abstract void setDeltaMovement(Vec3 vec3);
    @Shadow public abstract void unRide();
    @Shadow public abstract float getYRot();
    @Shadow public abstract EntityType<?> getType();
    @Shadow protected abstract void removeAfterChangingDimensions();
    @Shadow public abstract Vec3 position();
    @Shadow public abstract int getId();
    @Shadow public abstract void discard();
    @Shadow public abstract double getX();
    @Shadow public abstract double getY(double d);
    @Shadow public abstract double getZ();
    // @formatter:on
}
