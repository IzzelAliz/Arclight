package io.izzel.arclight.common.mixin.bukkit;

import com.google.common.base.Preconditions;
import io.izzel.arclight.common.mod.server.ArclightServer;
import org.bukkit.craftbukkit.block.CraftBlockType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.transformer.meta.MixinMerged;

import java.lang.reflect.Method;
import java.util.Arrays;

@Mixin(value = CraftBlockType.class, remap = false)
public class CraftBlockTypeMixin {

    /**
     * @author InitAuther97
     * @reason Skip mixin methods to avoid bumping into accessors or something
     */
    // Why do you tell me it should be transient? How can a method be transient?
    @Overwrite
    private static boolean hasMethod(Class<?> clazz, Class<?>... params) {
        boolean hasMethod = false;

        for (Method method : clazz.getDeclaredMethods()) {
            if (method.getDeclaredAnnotation(MixinMerged.class) != null) {
                continue;
            }
            if (Arrays.equals(method.getParameterTypes(), params)) {
                Preconditions.checkArgument(!hasMethod, "More than one matching method for %s, args %s", clazz, Arrays.toString(params));
                hasMethod = true;
            }
        }

        return hasMethod;
    }
}
