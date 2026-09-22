package io.izzel.arclight.common.mixin.core.network.chat;

import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;

@Mixin(Style.class)
public class StyleMixin {

    // @formatter:off
    @Shadow @Final @Nullable private TextColor color;
    @Shadow @Final @Nullable private Integer shadowColor;
    @Shadow @Final @Nullable private Boolean bold;
    @Shadow @Final @Nullable private Boolean italic;
    @Shadow @Final @Nullable private Boolean underlined;
    @Shadow @Final @Nullable private Boolean strikethrough;
    @Shadow @Final @Nullable private Boolean obfuscated;
    @Shadow @Final @Nullable private ClickEvent clickEvent;
    @Shadow @Final @Nullable private HoverEvent hoverEvent;
    @Shadow @Final @Nullable private String insertion;
    @Shadow @Final @Nullable private FontDescription font;
    // @formatter:on

    public Style setStrikethrough(final Boolean b) {
        return this.shadowColor == null && this.color == null && this.bold == null && this.italic == null && this.underlined == null && b == null && this.obfuscated == null && this.clickEvent == null && this.hoverEvent == null && this.insertion == null && this.font == null
            ? Style.EMPTY
            : (this.shadowColor == null ? Style.EMPTY : Style.EMPTY.withShadowColor(this.shadowColor)).withColor(this.color).withBold(this.bold).withItalic(this.italic).withUnderlined(this.underlined).withStrikethrough(b).withObfuscated(this.obfuscated).withClickEvent(this.clickEvent).withHoverEvent(this.hoverEvent).withInsertion(this.insertion).withFont(this.font);
    }

    public Style setUnderline(final Boolean b) {
        return this.shadowColor == null && this.color == null && this.bold == null && this.italic == null && b == null && this.strikethrough == null && this.obfuscated == null && this.clickEvent == null && this.hoverEvent == null && this.insertion == null && this.font == null
            ? Style.EMPTY
            : (this.shadowColor == null ? Style.EMPTY : Style.EMPTY.withShadowColor(this.shadowColor)).withColor(this.color).withBold(this.bold).withItalic(this.italic).withUnderlined(b).withStrikethrough(this.strikethrough).withObfuscated(this.obfuscated).withClickEvent(this.clickEvent).withHoverEvent(this.hoverEvent).withInsertion(this.insertion).withFont(this.font);
    }

    public Style setRandom(final Boolean b) {
        return this.shadowColor == null && this.color == null && this.bold == null && this.italic == null && this.underlined == null && this.strikethrough == null && b == null && this.clickEvent == null && this.hoverEvent == null && this.insertion == null && this.font == null
            ? Style.EMPTY
            : (this.shadowColor == null ? Style.EMPTY : Style.EMPTY.withShadowColor(this.shadowColor)).withColor(this.color).withBold(this.bold).withItalic(this.italic).withUnderlined(this.underlined).withStrikethrough(this.strikethrough).withObfuscated(b).withClickEvent(this.clickEvent).withHoverEvent(this.hoverEvent).withInsertion(this.insertion).withFont(this.font);
    }
}
