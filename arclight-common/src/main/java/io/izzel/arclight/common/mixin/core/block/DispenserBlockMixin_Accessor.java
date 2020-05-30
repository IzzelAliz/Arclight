package io.izzel.arclight.common.mixin.core.block;

import net.minecraft.block.DispenserBlock;
import net.minecraft.dispenser.IDispenseItemBehavior;
import net.minecraft.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(DispenserBlock.class)
public interface DispenserBlockMixin_Accessor {

    @Accessor("DISPENSE_BEHAVIOR_REGISTRY")
    static Map<Item, IDispenseItemBehavior> getDispenseBehaviorRegistry() {
        return null;
    }
}
