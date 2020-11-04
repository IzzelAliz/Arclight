package io.izzel.arclight.common.mixin.core.util.text;

import net.minecraft.util.text.Color;
import net.minecraft.util.text.TextFormatting;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(Color.class)
public class ColorMixin {

    // @formatter:off
    @Shadow @Final @Mutable @Nullable public String name;
    // @formatter:on

    public TextFormatting format;

    public void arclight$constructor(int color) {
        throw new RuntimeException();
    }

    public void arclight$constructor(int color, String name, TextFormatting textFormatting) {
        arclight$constructor(color);
        this.name = name;
        this.format = textFormatting;
    }

    @Inject(method = "<init>(ILjava/lang/String;)V", at = @At("RETURN"))
    private void arclight$withFormat(int color, String name, CallbackInfo ci) {
        this.format = TextFormatting.getValueByName(name);
    }
}
