package io.izzel.arclight.common.bridge.bukkit;

import io.izzel.arclight.i18n.conf.MaterialPropertySpec;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;

public interface MaterialBridge {

    void bridge$setupBlock(ResourceLocation key, Block block, MaterialPropertySpec spec);

    void bridge$setupItem(ResourceLocation key, Item item, MaterialPropertySpec spec);

    @Nullable
    MaterialPropertySpec bridge$getSpec();

    MaterialPropertySpec.MaterialType bridge$getType();
}
