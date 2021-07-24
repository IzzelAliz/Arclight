package io.izzel.arclight.common.mixin.core.world.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.ItemBasedSteering;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ItemBasedSteering.class)
public class ItemBaseSteeringMixin {

    // @formatter:off
    @Shadow public boolean boosting;
    @Shadow public int boostTime;
    @Shadow public int boostTimeTotal;
    @Shadow @Final private SynchedEntityData entityData;
    @Shadow @Final private EntityDataAccessor<Integer> boostTimeAccessor;
    // @formatter:on

    public void setBoostTicks(int ticks) {
        this.boosting = true;
        this.boostTime = 0;
        this.boostTimeTotal = ticks;
        this.entityData.set(this.boostTimeAccessor, this.boostTimeTotal);
    }
}
