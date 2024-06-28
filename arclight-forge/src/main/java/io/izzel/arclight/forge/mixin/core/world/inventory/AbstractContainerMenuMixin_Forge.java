package io.izzel.arclight.forge.mixin.core.world.inventory;

import io.izzel.arclight.common.bridge.core.inventory.container.ContainerBridge;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin_Forge implements ContainerBridge {

}
