package io.izzel.arclight.common.mixin.bukkit;

import net.minecraft.server.level.ServerBossEvent;
import org.bukkit.craftbukkit.boss.CraftBossBar;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CraftBossBar.class, remap = false)
public abstract class CraftBossBarMixin {
    @Shadow @Final private ServerBossEvent handle;

    @Inject(method = {"setTitle", "setColor", "setStyle"}, at = @At("RETURN"))
    private void arclight$markSaved(CallbackInfo ci) {
        if (this.handle instanceof net.minecraft.server.bossevents.CustomBossEvent custom) {
            custom.setDirty();
        }
    }
}
