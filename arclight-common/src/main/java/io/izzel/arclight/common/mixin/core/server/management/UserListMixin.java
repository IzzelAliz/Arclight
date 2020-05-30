package io.izzel.arclight.common.mixin.core.server.management;

import net.minecraft.server.management.UserList;
import net.minecraft.server.management.UserListEntry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import io.izzel.arclight.common.bridge.server.management.UserListBridge;

import java.util.Collection;
import java.util.Map;

@Mixin(UserList.class)
public class UserListMixin<K, V extends UserListEntry<K>> implements UserListBridge<V> {

    // @formatter:off
    @Shadow @Final private Map<String, V> values;
    // @formatter:on

    public Collection<V> getValues() {
        return this.values.values();
    }

    @Override
    public Collection<V> bridge$getValues() {
        return getValues();
    }
}
