package io.izzel.arclight.common.bridge.util.text;

import net.minecraft.util.text.ITextComponent;

import java.util.Iterator;
import java.util.stream.Stream;

public interface ITextComponentBridge {

    Stream<ITextComponent> bridge$stream();

    Iterator<ITextComponent> bridge$iterator();
}
