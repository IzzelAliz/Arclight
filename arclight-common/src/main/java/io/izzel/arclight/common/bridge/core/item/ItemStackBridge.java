package io.izzel.arclight.common.bridge.core.item;

import net.minecraft.nbt.CompoundTag;

public interface ItemStackBridge {

    void bridge$convertStack(int version);

    CompoundTag bridge$getForgeCaps();

    void bridge$setForgeCaps(CompoundTag caps);
}
