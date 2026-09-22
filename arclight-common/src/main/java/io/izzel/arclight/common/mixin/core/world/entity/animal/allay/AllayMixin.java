package io.izzel.arclight.common.mixin.core.world.entity.animal.allay;

import io.izzel.arclight.common.mixin.core.world.entity.MobMixin;
import io.izzel.arclight.common.mod.mixins.annotation.RenameInto;
import net.minecraft.network.syncher.EntityDataAccessor;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.common.bridge.core.world.level.WorldBridge;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.spongepowered.asm.mixin.injection.At;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.allay.Allay;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Allay.class)
public abstract class AllayMixin extends MobMixin {

    // @formatter:off
    @Shadow @Final private static EntityDataAccessor<Boolean> DATA_CAN_DUPLICATE;
    // @formatter:on

    public boolean forceDancing = false;

    public void setCanDuplicate(boolean canDuplicate) {
        this.entityData.set(DATA_CAN_DUPLICATE, canDuplicate);
    }

    private transient boolean arclight$duplicationAdded;

    @Decorate(method = "duplicateAllay()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean arclight$duplicateSpawn(Level level, Entity entity) throws Throwable {
        ((WorldBridge) level).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.DUPLICATION);
        boolean added = (boolean) DecorationOps.callsite().invoke(level, entity);
        this.arclight$duplicationAdded = added && !entity.isRemoved();
        return added;
    }

    @Decorate(method = "mobInteract", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/allay/Allay;duplicateAllay()V"))
    private void arclight$duplicateInteraction(Allay allay) throws Throwable {
        boolean previous = this.arclight$duplicationAdded;
        this.arclight$duplicationAdded = false;
        boolean added;
        try {
            DecorationOps.callsite().invoke(allay);
            added = this.arclight$duplicationAdded;
        } finally {
            this.arclight$duplicationAdded = previous;
        }
        if (!added) {
            DecorationOps.cancel().invoke((InteractionResult) InteractionResult.SUCCESS);
            return;
        }
    }

    @RenameInto("duplicateAllay")
    public Allay bukkit$duplicateAllay() {
        Allay allay = EntityType.ALLAY.create(this.level(), EntitySpawnReason.BREEDING);
        if (allay != null) {
            allay.snapTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot());
            allay.resetFallDistance();
            ((AllayMixin) (Object) allay).setDuplicationCooldown(6000L);
            this.setDuplicationCooldown(6000L);
            allay.setPersistenceRequired();
            if (!((io.izzel.arclight.common.bridge.core.world.level.IWorldWriterBridge) this.level())
                .bridge$addEntity(allay, org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.DUPLICATION)
                || allay.isRemoved()) {
                return null;
            }
        }
        return allay;
    }

    @Shadow public abstract void setDuplicationCooldown(long duplicationCooldown);
}
