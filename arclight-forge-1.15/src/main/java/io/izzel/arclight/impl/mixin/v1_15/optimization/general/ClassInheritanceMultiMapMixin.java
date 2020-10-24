package io.izzel.arclight.impl.mixin.v1_15.optimization.general;

import net.minecraft.util.ClassInheritanceMultiMap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Mixin(ClassInheritanceMultiMap.class)
public class ClassInheritanceMultiMapMixin<T> {

    // @formatter:off
    @Shadow @Final private Class<T> baseClass;
    @Shadow @Final private Map<Class<?>, List<T>> map;
    @Shadow @Final private List<T> values;
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public <S> Collection<S> getByClass(Class<S> p_219790_1_) {
        Collection<T> collection = this.map.get(p_219790_1_);
        if (collection == null) {
            collection = this.createList(p_219790_1_);
        }
        return (Collection<S>) Collections.unmodifiableCollection(collection);
    }

    private <S> Collection<T> createList(Class<S> p_219790_1_) {
        if (!this.baseClass.isAssignableFrom(p_219790_1_)) {
            throw new IllegalArgumentException("Don't know how to search for " + p_219790_1_);
        } else {
            List<T> list = new ArrayList<>();
            for (T value : this.values) {
                if (p_219790_1_.isInstance(value)) {
                    list.add(value);
                }
            }
            this.map.put(p_219790_1_, list);
            return list;
        }
    }
}
