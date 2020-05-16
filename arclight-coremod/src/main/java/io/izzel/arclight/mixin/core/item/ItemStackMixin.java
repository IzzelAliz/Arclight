package io.izzel.arclight.mixin.core.item;

import io.izzel.arclight.bridge.item.ItemStackBridge;
import net.minecraft.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.craftbukkit.v1_14_R1.util.CraftMagicNumbers;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin implements ItemStackBridge {

    private static final Logger LOG = LogManager.getLogger("Arclight");

    public void convertStack(int version) {
        if (0 < version && version < CraftMagicNumbers.INSTANCE.getDataVersion()) {
            LOG.warn("Legacy ItemStack being used, updates will not applied: {}", this);
        }
    }

    @Override
    public void bridge$convertStack(int version) {
        this.convertStack(version);
    }
}
