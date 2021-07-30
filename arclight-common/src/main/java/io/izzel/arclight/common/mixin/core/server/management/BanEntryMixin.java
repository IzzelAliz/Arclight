package io.izzel.arclight.common.mixin.core.server.management;

import io.izzel.arclight.common.bridge.core.server.management.BanEntryBridge;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Date;
import net.minecraft.server.players.BanListEntry;

@Mixin(BanListEntry.class)
public class BanEntryMixin implements BanEntryBridge {

    // @formatter:off
    @Shadow @Final protected Date created;
    // @formatter:on

    public Date getCreated() {
        return this.created;
    }

    @Override
    public Date bridge$getCreated() {
        return getCreated();
    }
}
