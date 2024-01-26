package io.izzel.arclight.common.mixin.core;

import io.izzel.arclight.api.ArclightVersion;
import net.minecraft.CrashReport;
import net.minecraft.SystemReport;
import org.bukkit.craftbukkit.v.CraftCrashReport;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CrashReport.class)
public class CrashReportMixin {

    @Shadow @Final private SystemReport systemReport;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void arclight$additional(String string, Throwable throwable, CallbackInfo ci) {
        this.systemReport.setDetail("Arclight Release", ArclightVersion.current()::getReleaseName);
        this.systemReport.setDetail("Arclight", new CraftCrashReport());
    }
}
