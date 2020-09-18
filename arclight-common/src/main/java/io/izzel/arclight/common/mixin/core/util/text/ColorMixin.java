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

    public void arclight$constructor(int p_i232573_1_) {
        throw new RuntimeException();
    }

    public void arclight$constructor(int p_i232573_1_, String p_i232573_2_, TextFormatting textFormatting) {
        arclight$constructor(p_i232573_1_);
        this.name = p_i232573_2_;
        this.format = textFormatting;
    }

    @Inject(method = "<init>(ILjava/lang/String;)V", at = @At("RETURN"))
    private void arclight$withFormat(int p_i232573_1_, String name, CallbackInfo ci) {
        this.format = TextFormatting.getValueByName(name);
    }
}
