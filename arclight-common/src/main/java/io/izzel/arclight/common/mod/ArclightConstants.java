package io.izzel.arclight.common.mod;

import com.google.common.collect.ImmutableList;
import io.izzel.arclight.api.EnumHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Unit;
import net.minecraft.world.server.TicketType;
import net.minecraft.world.storage.loot.LootParameter;
import org.bukkit.TreeType;
import org.bukkit.plugin.Plugin;

import java.util.Comparator;

public class ArclightConstants {

    public static final TicketType<Unit> PLUGIN = TicketType.create("plugin", (a, b) -> 0);
    public static final TicketType<Plugin> PLUGIN_TICKET = TicketType.create("plugin_ticket", Comparator.comparing(it -> it.getClass().getName()));

    public static final TreeType MOD = EnumHelper.addEnum(TreeType.class, "MOD", ImmutableList.of(), ImmutableList.of());

    public static final LootParameter<Integer> LOOTING_MOD = new LootParameter<>(new ResourceLocation("bukkit:looting_mod"));

    /**
     * Arclight marker magic value for non-used custom dimension
     */
    public static final int ARCLIGHT_DIMENSION = 0xA2c11947;

    public static int currentTick;

}
