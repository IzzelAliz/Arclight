package io.izzel.arclight.common.mixin.bukkit;

import org.bukkit.event.block.Action;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@SuppressWarnings({"ConstantConditions", "unused"})
@Mixin(value = Action.class, remap = false)
public class ActionMixin {
    // @formatter:off
    @Shadow @Final public static Action LEFT_CLICK_AIR;
    @Shadow @Final public static Action LEFT_CLICK_BLOCK;
    @Shadow @Final public static Action RIGHT_CLICK_AIR;
    @Shadow @Final public static Action RIGHT_CLICK_BLOCK;
    // @formatter:on

    public boolean isLeftClick() {
        return (Object) this == LEFT_CLICK_AIR || (Object) this == LEFT_CLICK_BLOCK;
    }

    public boolean isRightClick() {
        return (Object) this == RIGHT_CLICK_AIR || (Object) this == RIGHT_CLICK_BLOCK;
    }
}
