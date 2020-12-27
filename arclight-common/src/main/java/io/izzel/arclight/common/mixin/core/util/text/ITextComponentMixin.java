package io.izzel.arclight.common.mixin.core.util.text;

import com.google.common.collect.Streams;
import io.izzel.arclight.common.bridge.util.text.ITextComponentBridge;
import net.minecraft.util.text.ITextComponent;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

@Mixin(ITextComponent.class)
public interface ITextComponentMixin extends ITextComponentBridge, Iterable<ITextComponent> {

    // @formatter:off
    @Shadow List<ITextComponent> getSiblings();
    // @formatter:on

    default Stream<ITextComponent> stream() {
        class Func implements Function<ITextComponent, Stream<? extends ITextComponent>> {

            @Override
            public Stream<? extends ITextComponent> apply(ITextComponent component) {
                return ((ITextComponentBridge) component).bridge$stream();
            }
        }
        return Streams.concat(Stream.of((ITextComponent) this), this.getSiblings().stream().flatMap(new Func()));
    }

    @Override
    default @NotNull Iterator<ITextComponent> iterator() {
        return this.stream().iterator();
    }

    @Override
    default Stream<ITextComponent> bridge$stream() {
        return stream();
    }

    @Override
    default Iterator<ITextComponent> bridge$iterator() {
        return iterator();
    }
}
