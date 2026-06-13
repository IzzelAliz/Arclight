package io.izzel.arclight.common.mixin.core.network.chat;

import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Style.class)
public class StyleMixin {

    public Style setStrikethrough(final Boolean b) {
        return ((Style) (Object) this).withStrikethrough(b);
    }

    public Style setUnderline(final Boolean b) {
        return ((Style) (Object) this).withUnderlined(b);
    }

    public Style setRandom(final Boolean b) {
        return ((Style) (Object) this).withObfuscated(b);
    }
}
