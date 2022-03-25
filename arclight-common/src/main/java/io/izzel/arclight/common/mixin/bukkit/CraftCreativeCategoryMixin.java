package io.izzel.arclight.common.mixin.bukkit;

import net.minecraft.world.item.CreativeModeTab;
import org.bukkit.craftbukkit.v.inventory.CraftCreativeCategory;
import org.bukkit.inventory.CreativeCategory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;

@Mixin(CraftCreativeCategory.class)
public class CraftCreativeCategoryMixin {

    @Shadow @Final @Mutable private static Map<CreativeModeTab, CreativeCategory> NMS_TO_BUKKIT;
}
