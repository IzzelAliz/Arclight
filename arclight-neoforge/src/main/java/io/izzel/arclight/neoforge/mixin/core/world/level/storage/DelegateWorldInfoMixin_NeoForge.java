package io.izzel.arclight.neoforge.mixin.core.world.level.storage;

import io.izzel.arclight.common.mod.util.DelegateWorldInfo;
import net.minecraft.world.level.storage.ServerLevelData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = DelegateWorldInfo.class, remap = false)
public abstract class DelegateWorldInfoMixin_NeoForge {

    @Shadow @Final private ServerLevelData serverLevelData;

    public float getDayTimeFraction() {
        return serverLevelData.getDayTimeFraction();
    }

    public float getDayTimePerTick() {
        return serverLevelData.getDayTimePerTick();
    }

    public void setDayTimeFraction(float fraction) {
        serverLevelData.setDayTimeFraction(fraction);
    }

    public void setDayTimePerTick(float perTick) {
        serverLevelData.setDayTimePerTick(perTick);
    }
}
