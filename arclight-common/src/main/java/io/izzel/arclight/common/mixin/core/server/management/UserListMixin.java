package io.izzel.arclight.common.mixin.core.server.management;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import io.izzel.arclight.common.bridge.core.server.management.UserListBridge;

import java.util.Collection;
import java.util.Map;
import net.minecraft.server.players.StoredUserEntry;
import net.minecraft.server.players.StoredUserList;

@Mixin(StoredUserList.class)
public class UserListMixin<K, V extends StoredUserEntry<K>> implements UserListBridge<V> {

    // @formatter:off
    @Shadow @Final private Map<String, V> map;
    // @formatter:on

    public Collection<V> getValues() {
        return this.map.values();
    }

    @Override
    public Collection<V> bridge$getValues() {
        return getValues();
    }
}
