package io.izzel.arclight.common.mixin.core.network.play.client;

import net.minecraft.network.play.client.CCloseWindowPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(CCloseWindowPacket.class)
public class CCloseWindowPacketMixin {

    @Shadow private int windowId;

    public void arclight$constructor() {
        throw new RuntimeException();
    }

    public void arclight$constructor(int id) {
        arclight$constructor();
        this.windowId = id;
    }
}
