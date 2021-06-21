package io.izzel.arclight.impl.mixin.optimization.general;

import net.minecraft.util.ClassInheritanceMultiMap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mixin(ClassInheritanceMultiMap.class)
public class ClassInheritanceMultiMapMixin<T> {

    // @formatter:off
    @Shadow @Final private Class<T> baseClass;
    @Shadow @Final @Mutable private Map<Class<?>, List<T>> map;
    @Shadow @Final @Mutable private List<T> values;
    // @formatter:on

    private static final ArrayList<?> EMPTY_LIST = new ArrayList<>();

    @Redirect(method = "<init>", at = @At(value = "INVOKE", remap = false, target = "Lcom/google/common/collect/Maps;newHashMap()Ljava/util/HashMap;"))
    private HashMap<Class<?>, List<T>> optimization$dropClass() {
        return null;
    }

    @SuppressWarnings("unchecked")
    @Redirect(method = "<init>", at = @At(value = "INVOKE", remap = false, target = "Lcom/google/common/collect/Lists;newArrayList()Ljava/util/ArrayList;"))
    private ArrayList<T> optimization$dropList() {
        return (ArrayList<T>) EMPTY_LIST;
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", remap = false, target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object optimization$dropPut(Map<?, ?> map, Object key, Object value) {
        return null;
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public boolean add(T p_add_1_) {
        if (map != null) {
            boolean flag = false;

            for (Map.Entry<Class<?>, List<T>> entry : this.map.entrySet()) {
                if (entry.getKey().isInstance(p_add_1_)) {
                    flag |= entry.getValue().add(p_add_1_);
                }
            }

            return flag;
        } else {
            map = new HashMap<>();
            values = new ArrayList<>();
            values.add(p_add_1_);
            map.put(baseClass, values);
            return true;
        }
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public boolean remove(Object p_remove_1_) {
        if (map == null) return false;
        boolean flag = false;

        for (Map.Entry<Class<?>, List<T>> entry : this.map.entrySet()) {
            if (entry.getKey().isInstance(p_remove_1_)) {
                List<T> list = entry.getValue();
                flag |= list.remove(p_remove_1_);
            }
        }

        return flag;
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public boolean contains(Object p_contains_1_) {
        return map != null && this.getByClass(p_contains_1_.getClass()).contains(p_contains_1_);
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public <S> Collection<S> getByClass(Class<S> p_219790_1_) {
        if (p_219790_1_ == baseClass) {
            return (Collection<S>) Collections.unmodifiableCollection(values);
        }
        if (map == null) {
            return Collections.emptyList();
        }
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
