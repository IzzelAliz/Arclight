package io.izzel.arclight.common.mixin.core.tags;

import io.izzel.arclight.common.bridge.tags.NetworkTagCollectionBridge;
import net.minecraft.tags.NetworkTagCollection;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(NetworkTagCollection.class)
public class NetworkTagCollectionMixin implements NetworkTagCollectionBridge {

    public int version;

    @Override
    public void bridge$increaseTag() {
        this.version++;
    }
}
