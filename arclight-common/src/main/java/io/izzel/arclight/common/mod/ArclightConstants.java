package io.izzel.arclight.common.mod;

import com.google.common.collect.ImmutableList;
import io.izzel.arclight.api.EnumHelper;
import io.izzel.arclight.api.Unsafe;
import net.minecraft.util.Unit;
import net.minecraft.world.server.TicketType;
import net.minecraft.world.storage.loot.LootParameter;
import net.minecraft.world.storage.loot.LootParameters;
import org.bukkit.TreeType;
import org.bukkit.plugin.Plugin;

public class ArclightConstants {

    public static final TicketType<Unit> PLUGIN = Unsafe.getStatic(TicketType.class, "PLUGIN");
    public static final TicketType<Plugin> PLUGIN_TICKET = Unsafe.getStatic(TicketType.class, "PLUGIN_TICKET");

    public static final TreeType MOD = EnumHelper.addEnum(TreeType.class, "MOD", ImmutableList.of(), ImmutableList.of());

    public static final LootParameter<Integer> LOOTING_MOD = Unsafe.getStatic(LootParameters.class, "LOOTING_MOD");

    /**
     * Arclight marker magic value for non-used custom dimension
     */
    public static final int ARCLIGHT_DIMENSION = 0xA2c11947;

    public static int currentTick;

}
