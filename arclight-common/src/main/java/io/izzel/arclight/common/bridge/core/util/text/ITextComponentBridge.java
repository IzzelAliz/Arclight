package io.izzel.arclight.common.bridge.core.util.text;

import java.util.Iterator;
import java.util.stream.Stream;
import net.minecraft.network.chat.Component;

public interface ITextComponentBridge {

    Stream<Component> bridge$stream();

    Iterator<Component> bridge$iterator();
}
