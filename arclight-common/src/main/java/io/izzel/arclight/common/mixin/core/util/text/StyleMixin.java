package io.izzel.arclight.common.mixin.core.util.text;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.Color;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;

@Mixin(Style.class)
public class StyleMixin {

    // @formatter:off
    @Shadow @Final @Nullable private Color color;
    @Shadow @Final @Nullable private Boolean bold;
    @Shadow @Final @Nullable private Boolean italic;
    @Shadow @Final @Nullable private Boolean underlined;
    @Shadow @Final @Nullable private Boolean strikethrough;
    @Shadow @Final @Nullable private Boolean obfuscated;
    @Shadow @Final @Nullable private ClickEvent clickEvent;
    @Shadow @Final @Nullable private HoverEvent hoverEvent;
    @Shadow @Final @Nullable private String insertion;
    @Shadow @Final @Nullable private ResourceLocation fontId;
    // @formatter:on

    public Style setStrikethrough(final Boolean b) {
        return new Style(this.color, this.bold, this.italic, this.underlined, b, this.obfuscated, this.clickEvent, this.hoverEvent, this.insertion, this.fontId);
    }

    public Style setUnderline(final Boolean b) {
        return new Style(this.color, this.bold, this.italic, b, this.strikethrough, this.obfuscated, this.clickEvent, this.hoverEvent, this.insertion, this.fontId);
    }

    public Style setRandom(final Boolean b) {
        return new Style(this.color, this.bold, this.italic, this.underlined, this.strikethrough, b, this.clickEvent, this.hoverEvent, this.insertion, this.fontId);
    }
}
