package io.izzel.arclight.neoforge.mixin.core.world.inventory;

import io.izzel.arclight.common.bridge.core.world.inventory.AbstractContainerMenuBridge;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin_NeoForge implements AbstractContainerMenuBridge {

}
