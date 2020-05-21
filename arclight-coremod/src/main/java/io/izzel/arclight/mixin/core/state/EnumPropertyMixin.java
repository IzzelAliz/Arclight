package io.izzel.arclight.mixin.core.state;

import net.minecraft.state.EnumProperty;
import net.minecraft.util.IStringSerializable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

@Mixin(EnumProperty.class)
public abstract class EnumPropertyMixin {

    @Shadow
    public static <T extends Enum<T> & IStringSerializable> EnumProperty<T> create(String name, Class<T> clazz, Collection<T> values) {
        return null;
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public static <T extends Enum<T> & IStringSerializable> EnumProperty<T> create(String name, Class<T> clazz, Predicate<T> filter) {
        try {
            List<T> list = new ArrayList<>();
            for (T enumConstant : clazz.getEnumConstants()) {
                if (filter.test(enumConstant)) list.add(enumConstant);
            }
            return create(name, clazz, list);
        } catch (Throwable t) {
            System.out.println(name);
            System.out.println(clazz);
            System.out.println(filter);
            for (T constant : clazz.getEnumConstants()) {
                System.out.println(constant);
            }
            t.printStackTrace();
            throw t;
        }
    }
}
