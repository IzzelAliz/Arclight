package io.izzel.arclight.forge.mixin.core.world.entity;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.tools.product.Product;
import io.izzel.tools.product.Product4;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.extensions.IForgeEntity;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Collection;

@Mixin(Entity.class)
public abstract class EntityMixin_Forge implements EntityBridge, IForgeEntity {

    // @formatter:off
    @Shadow public abstract Level level();
    @Shadow(remap = false) public abstract Collection<ItemEntity> captureDrops(Collection<ItemEntity> par1);
    @Shadow(remap = false) public abstract void revive();
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

    @Decorate(method = "spawnAtLocation(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/entity/item/ItemEntity;", at = @At(value = "INVOKE", remap = false, ordinal = 0, target = "Lnet/minecraft/world/entity/Entity;captureDrops()Ljava/util/Collection;"))
    public Collection<ItemEntity> arclight$forceDrops(Entity entity) throws Throwable {
        if (this.bridge$isForceDrops()) {
            return null;
        }
        return (Collection<ItemEntity>) DecorationOps.callsite().invoke(entity);
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
    public Product4<Boolean, Double, Double, Double> bridge$onEntityTeleportCommand(ServerLevel level, double x, double y, double z) {
        EntityTeleportEvent.TeleportCommand event = ForgeEventFactory.onEntityTeleportCommand((Entity) (Object) this, x, y, z);
        // Forge 64 signals cancellation by returning null, not a mutable event flag.
        return event == null ? Product.of(true, x, y, z)
            : Product.of(false, event.getTargetX(), event.getTargetY(), event.getTargetZ());
    }

    @Override
    public boolean bridge$forge$canUpdate() {
        return this.canUpdate();
    }
}
