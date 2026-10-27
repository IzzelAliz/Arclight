package io.izzel.arclight.common.bridge.core.world.entity.ai.attributes;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;

public interface AttributeMapBridge {
    void bridge$registerAttribute(Holder<Attribute> attribute);
}
