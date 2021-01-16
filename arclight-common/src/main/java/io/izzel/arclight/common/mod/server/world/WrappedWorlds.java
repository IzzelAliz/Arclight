package io.izzel.arclight.common.mod.server.world;

import io.izzel.arclight.common.mod.ArclightMod;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.function.Function;

public class WrappedWorlds {

    private static final HashSet<Class<?>> RESULT = new HashSet<>();
    private static final HashMap<Class<?>, Field> FIELD = new HashMap<>();

    public static Optional<Field> getDelegate(Class<?> cl) {
        if (cl.equals(ServerWorld.class)) {
            return Optional.empty();
        } else {
            return getOrCreate(cl, key -> {
                for (Field f : cl.getDeclaredFields()) {
                    if (World.class.isAssignableFrom(f.getType())) {
                        ArclightMod.LOGGER.debug("{} delegates to field {}", cl, f.getName());
                        f.setAccessible(true);
                        return f;
                    }
                }
                Optional<Field> delegate = getDelegate(cl);
                if (delegate.isPresent()) {
                    return delegate.get();
                } else {
                    ArclightMod.LOGGER.debug("{} delegates to nothing", cl);
                    return null;
                }
            });
        }
    }

    private static Optional<Field> getOrCreate(Class<?> cl, Function<Class<?>, Field> function) {
        Field field = FIELD.get(cl);
        if (field != null) {
            return Optional.of(field);
        } else if (RESULT.contains(cl)) {
            return Optional.empty();
        } else {
            Field delegate = function.apply(cl);
            RESULT.add(cl);
            if (delegate != null) {
                FIELD.put(cl, delegate);
            }
            return Optional.ofNullable(delegate);
        }
    }
}
