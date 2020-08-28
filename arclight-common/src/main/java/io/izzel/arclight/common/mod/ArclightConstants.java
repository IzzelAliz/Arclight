package io.izzel.arclight.common.mod;

import com.google.common.collect.ImmutableList;
import io.izzel.arclight.api.EnumHelper;
import net.minecraft.loot.LootParameter;
import net.minecraft.util.ResourceLocation;
import org.bukkit.TreeType;

public class ArclightConstants {

    public static final TreeType MOD = EnumHelper.addEnum(TreeType.class, "MOD", ImmutableList.of(), ImmutableList.of());

    public static final LootParameter<Integer> LOOTING_MOD = new LootParameter<>(new ResourceLocation("bukkit:looting_mod"));

    /**
     * Arclight marker magic value for non-used custom dimension
     */
    public static final int ARCLIGHT_DIMENSION = 0xA2c11947;

    public static int currentTick;

}
