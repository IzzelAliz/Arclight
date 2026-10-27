package io.izzel.arclight.common.bridge.core.world.level.block.entity;

public interface JukeboxBlockEntityBridge {
    default void setSongItemWithoutPlaying(net.minecraft.world.item.ItemStack itemStack, long ticksSinceSongStarted) {
        throw new UnsupportedOperationException("Implemented by JukeboxBlockEntityMixin");
    }
}
