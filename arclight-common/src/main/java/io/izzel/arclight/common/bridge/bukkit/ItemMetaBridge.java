package io.izzel.arclight.common.bridge.bukkit;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;

import java.util.Map;

public interface ItemMetaBridge {

    CompoundNBT bridge$getForgeCaps();

    void bridge$setForgeCaps(CompoundNBT nbt);

    void bridge$offerUnhandledTags(CompoundNBT nbt);

    Map<String, INBT> bridge$getUnhandledTags();

    void bridge$setUnhandledTags(Map<String, INBT> tags);
}
