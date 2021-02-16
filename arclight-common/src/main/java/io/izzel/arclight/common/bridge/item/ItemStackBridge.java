package io.izzel.arclight.common.bridge.item;

import net.minecraft.nbt.CompoundNBT;

public interface ItemStackBridge {

    void bridge$convertStack(int version);

    CompoundNBT bridge$getForgeCaps();

    void bridge$setForgeCaps(CompoundNBT caps);
}
