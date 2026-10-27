package io.izzel.arclight.common.mixin.core.world.entity.ai.attributes;

import io.izzel.arclight.common.bridge.core.world.entity.ai.attributes.AttributeMapBridge;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import java.util.Map;

@Mixin(AttributeMap.class)
public abstract class AttributeMapMixin implements AttributeMapBridge {
    @Shadow @Final private Map<Holder<Attribute>, AttributeInstance> attributes;

    @Override
    public void bridge$registerAttribute(Holder<Attribute> attribute) {
        this.attributes.put(attribute, new AttributeInstance(attribute, AttributeInstance::getAttribute));
    }
}
