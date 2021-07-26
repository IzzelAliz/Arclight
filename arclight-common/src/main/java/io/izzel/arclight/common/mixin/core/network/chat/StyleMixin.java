package io.izzel.arclight.common.mixin.core.network.chat;

import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;

@Mixin(Style.class)
public class StyleMixin {

    // @formatter:off
    @Shadow @Final @Nullable private TextColor color;
    @Shadow @Final @Nullable private Boolean bold;
    @Shadow @Final @Nullable private Boolean italic;
    @Shadow @Final @Nullable private Boolean underlined;
    @Shadow @Final @Nullable private Boolean strikethrough;
    @Shadow @Final @Nullable private Boolean obfuscated;
    @Shadow @Final @Nullable private ClickEvent clickEvent;
    @Shadow @Final @Nullable private HoverEvent hoverEvent;
    @Shadow @Final @Nullable private String insertion;
    @Shadow @Final @Nullable private ResourceLocation font;
    // @formatter:on

    public Style setStrikethrough(final Boolean b) {
        return new Style(this.color, this.bold, this.italic, this.underlined, b, this.obfuscated, this.clickEvent, this.hoverEvent, this.insertion, this.font);
    }

    public Style setUnderline(final Boolean b) {
        return new Style(this.color, this.bold, this.italic, b, this.strikethrough, this.obfuscated, this.clickEvent, this.hoverEvent, this.insertion, this.font);
    }

    public Style setRandom(final Boolean b) {
        return new Style(this.color, this.bold, this.italic, this.underlined, this.strikethrough, b, this.clickEvent, this.hoverEvent, this.insertion, this.font);
    }
}
