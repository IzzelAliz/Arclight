package io.izzel.arclight.common.mixin.core.server.management;

import io.izzel.arclight.common.bridge.server.management.BanEntryBridge;
import net.minecraft.server.management.BanEntry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Date;

@Mixin(BanEntry.class)
public class BanEntryMixin implements BanEntryBridge {

    // @formatter:off
    @Shadow @Final protected Date banStartDate;
    // @formatter:on

    public Date getCreated() {
        return this.banStartDate;
    }

    @Override
    public Date bridge$getCreated() {
        return getCreated();
    }
}
