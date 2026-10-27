package io.izzel.arclight.common.mixin.core.core.component;

import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Optional;

@Mixin(DataComponentPatch.Builder.class)
public class DataComponentPatch_BuilderMixin implements io.izzel.arclight.common.bridge.core.core.component.DataComponentPatchBuilderBridge {

    @Shadow @Final public Reference2ObjectMap<DataComponentType<?>, Optional<?>> map;

    @Override
    public void copy(DataComponentPatch orig) {
        this.map.putAll(orig.map);
    }

    @Override
    public void clear(DataComponentType<?> type) {
        this.map.remove(type);
    }

    @Override
    public boolean isSet(DataComponentType<?> type) {
        return this.map.containsKey(type);
    }

    public boolean isEmpty() {
        return this.map.isEmpty();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (object instanceof DataComponentPatch.Builder patch) {
            return this.map.equals(patch.map);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return this.map.hashCode();
    }
}
