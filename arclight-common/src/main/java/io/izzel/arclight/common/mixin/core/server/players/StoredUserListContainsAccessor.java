package io.izzel.arclight.common.mixin.core.server.players;

import net.minecraft.server.players.StoredUserList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(StoredUserList.class)
public interface StoredUserListContainsAccessor {

    @Invoker("contains")
    boolean arclight$contains(Object profile);
}
