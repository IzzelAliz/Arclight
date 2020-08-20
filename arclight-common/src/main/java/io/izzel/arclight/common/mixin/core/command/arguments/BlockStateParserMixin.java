package io.izzel.arclight.common.mixin.core.command.arguments;

import com.mojang.brigadier.StringReader;
import net.minecraft.command.arguments.BlockStateParser;
import net.minecraft.state.Property;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.LinkedHashMap;
import java.util.Map;

@Mixin(BlockStateParser.class)
public class BlockStateParserMixin {

    // @formatter:off
    @Shadow @Final @Mutable private Map<Property<?>, Comparable<?>> properties;
    // @formatter:on

    @Inject(method = "<init>", at = @At("RETURN"))
    private void arclight$init(StringReader readerIn, boolean allowTags, CallbackInfo ci) {
        this.properties = new LinkedHashMap<>(properties);
    }
}
