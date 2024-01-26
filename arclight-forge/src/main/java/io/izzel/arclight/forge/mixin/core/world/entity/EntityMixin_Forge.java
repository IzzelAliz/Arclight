package io.izzel.arclight.forge.mixin.core.world.entity;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.entity.LivingEntityBridge;
import io.izzel.arclight.common.bridge.core.world.WorldBridge;
import io.izzel.arclight.common.bridge.core.world.level.block.PortalInfoBridge;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import io.izzel.tools.product.Product;
import io.izzel.tools.product.Product4;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.extensions.IForgeEntity;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import javax.annotation.Nullable;
import java.util.Collection;

@Mixin(Entity.class)
public abstract class EntityMixin_Forge implements EntityBridge, IForgeEntity {

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
    @Shadow(remap = false) public abstract Collection<ItemEntity> captureDrops(Collection<ItemEntity> par1);
    @Shadow(remap = false) public abstract void revive();
    @Shadow public abstract Vec3 position();
    @Shadow public abstract int getId();
    @Shadow public abstract void discard();
    @Shadow public abstract double getX();
    @Shadow public abstract double getY(double d);
    @Shadow public abstract double getZ();
    @Shadow(remap = false) public abstract boolean canUpdate();
    // @formatter:on

    @Override
    public void bridge$revive() {
        this.revive();
    }

    @Redirect(method = "updateFluidHeightAndDoFluidPushing(Ljava/util/function/Predicate;)V", remap = false, at = @At(value = "INVOKE", remap = true, target = "Lnet/minecraft/world/level/material/FluidState;getFlow(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 arclight$setLava(FluidState instance, BlockGetter level, BlockPos pos) {
        if (instance.getType().is(FluidTags.LAVA)) {
            this.bridge$setLastLavaContact(pos.immutable());
        }
        return instance.getFlow(level, pos);
    }

    @Redirect(method = "spawnAtLocation(Lnet/minecraft/world/item/ItemStack;F)Lnet/minecraft/world/entity/item/ItemEntity;", at = @At(value = "INVOKE", remap = false, ordinal = 0, target = "Lnet/minecraft/world/entity/Entity;captureDrops()Ljava/util/Collection;"))
    public Collection<ItemEntity> arclight$forceDrops(Entity entity) {
        Collection<ItemEntity> drops = entity.captureDrops();
        if (this instanceof LivingEntityBridge && ((LivingEntityBridge) this).bridge$isForceDrops()) {
            drops = null;
        }
        return drops;
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    @Nullable
    public Entity changeDimension(ServerLevel arg) {
        return this.changeDimension(arg, arg.getPortalForcer());
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite(remap = false)
    @Nullable
    public Entity changeDimension(ServerLevel server, net.minecraftforge.common.util.ITeleporter teleporter) {
        if (ForgeEventFactory.onTravelToDimension((Entity) (Object) this, server.dimension()))
            return null;
        if (this.level() instanceof ServerLevel && !this.isRemoved()) {
            this.level().getProfiler().push("changeDimension");
            if (server == null) {
                return null;
            }
            this.level().getProfiler().push("reposition");
            var bukkitPos = bridge$getLastTpPos();
            PortalInfo portalinfo = bukkitPos == null ? teleporter.getPortalInfo((Entity) (Object) this, server, this::findDimensionEntryPoint)
                : new PortalInfo(new Vec3(bukkitPos.x(), bukkitPos.y(), bukkitPos.z()), Vec3.ZERO, this.yRot, this.xRot);
            if (portalinfo == null) {
                return null;
            } else {
                ServerLevel world = ((PortalInfoBridge) portalinfo).bridge$getWorld() == null ? server : ((PortalInfoBridge) portalinfo).bridge$getWorld();
                if (world == this.level()) {
                    this.moveTo(portalinfo.pos.x, portalinfo.pos.y, portalinfo.pos.z, portalinfo.yRot, this.getXRot());
                    this.setDeltaMovement(portalinfo.speed);
                    return (Entity) (Object) this;
                }
                this.unRide();
                Entity transportedEntity = teleporter.placeEntity((Entity) (Object) this, (ServerLevel) this.level(), world, this.getYRot(), spawnPortal -> { //Forge: Start vanilla logic
                    this.level().getProfiler().popPush("reloading");
                    Entity entity = this.getType().create(world);
                    if (entity != null) {
                        entity.restoreFrom((Entity) (Object) this);
                        entity.moveTo(portalinfo.pos.x, portalinfo.pos.y, portalinfo.pos.z, portalinfo.yRot, entity.getXRot());
                        entity.setDeltaMovement(portalinfo.speed);
                        if (this.bridge$isInWorld()) {
                            world.addDuringTeleport(entity);
                            if (((WorldBridge) world).bridge$getTypeKey() == LevelStem.END) {
                                ArclightCaptures.captureEndPortalEntity((Entity) (Object) this, spawnPortal);
                                ServerLevel.makeObsidianPlatform(world);
                            }
                        }
                    }
                    return entity;
                }); //Forge: End vanilla logic

                this.removeAfterChangingDimensions();
                this.level().getProfiler().pop();
                ((ServerLevel) this.level()).resetEmptyTime();
                world.resetEmptyTime();
                this.level().getProfiler().pop();
                return transportedEntity;
            }
        } else {
            return null;
        }
    }

    @Override
    public boolean bridge$forge$isPartEntity() {
        return (Object) this instanceof PartEntity<?>;
    }

    @Override
    public Entity bridge$forge$getParent() {
        return ((PartEntity<?>) (Object) this).getParent();
    }

    @Override
    public Entity[] bridge$forge$getParts() {
        return this.getParts();
    }

    @Override
    public Product4<Boolean, Double, Double, Double> bridge$onEntityTeleportCommand(double x, double y, double z) {
        EntityTeleportEvent.TeleportCommand event = ForgeEventFactory.onEntityTeleportCommand((Entity) (Object) this, x, y, z);
        return Product.of(event.isCanceled(), event.getTargetX(), event.getTargetY(), event.getTargetZ());
    }

    @Override
    public boolean bridge$forge$canUpdate() {
        return this.canUpdate();
    }
}
