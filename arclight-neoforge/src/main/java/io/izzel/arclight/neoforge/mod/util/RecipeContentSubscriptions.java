package io.izzel.arclight.neoforge.mod.util;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.common.CommonHooks;

/** Remembers the final native subscription per live connection, not per world or player name. */
public final class RecipeContentSubscriptions {
    private static final Map<ServerGamePacketListenerImpl, Set<RecipeType<?>>> TYPES =
        Collections.synchronizedMap(new WeakHashMap<>());

    private RecipeContentSubscriptions() {}

    public static void capture(ServerPlayer player, Set<RecipeType<?>> types) {
        if (player.connection.getConnectionType().isNeoForge()) {
            TYPES.put(player.connection, Set.copyOf(types));
        }
    }

    public static void refresh(ServerPlayer player, RecipeMap recipes) {
        Set<RecipeType<?>> types = TYPES.get(player.connection);
        if (types != null && !types.isEmpty()) {
            CommonHooks.sendRecipes(player, types, recipes);
        }
    }
}
