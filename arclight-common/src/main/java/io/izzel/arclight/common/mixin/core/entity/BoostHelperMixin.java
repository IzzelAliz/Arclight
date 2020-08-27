package io.izzel.arclight.common.mixin.core.entity;

import net.minecraft.entity.BoostHelper;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.EntityDataManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BoostHelper.class)
public class BoostHelperMixin {

    // @formatter:off
    @Shadow public boolean saddledRaw;
    @Shadow public int field_233611_b_;
    @Shadow public int boostTimeRaw;
    @Shadow @Final private EntityDataManager manager;
    @Shadow @Final private DataParameter<Integer> boostTime;
    // @formatter:on

    public void setBoostTicks(int ticks) {
        this.saddledRaw = true;
        this.field_233611_b_ = 0;
        this.boostTimeRaw = ticks;
        this.manager.set(this.boostTime, this.boostTimeRaw);
    }
}
