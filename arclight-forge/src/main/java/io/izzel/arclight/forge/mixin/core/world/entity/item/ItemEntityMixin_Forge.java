package io.izzel.arclight.forge.mixin.core.world.entity.item;

import io.izzel.arclight.common.bridge.core.entity.item.ItemEntityBridge;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ItemEntity.class)
public class ItemEntityMixin_Forge implements ItemEntityBridge {
}
