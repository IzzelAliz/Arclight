package io.izzel.arclight.common.mod;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.DSL;
import io.izzel.arclight.api.EnumHelper;
import io.izzel.arclight.api.Unsafe;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.bukkit.TreeType;

import java.util.List;

public class ArclightConstants {

    public static final TreeType MOD = EnumHelper.addEnum(TreeType.class, "MOD", ImmutableList.of(), ImmutableList.of());

    public static final Level.ExplosionInteraction STANDARD = Unsafe.getStatic(Level.ExplosionInteraction.class, "STANDARD");
    public static final LootContextParam<Integer> LOOTING_MOD = Unsafe.getStatic(LootContextParams.class, "LOOTING_MOD");

    private static final DSL.TypeReference PDC_TYPE = () -> "bukkit_pdc";
    public static final DataFixTypes BUKKIT_PDC = EnumHelper.makeEnum(DataFixTypes.class, "BUKKIT_PDC", 0, List.of(DSL.TypeReference.class), List.of(PDC_TYPE));

    /**
     * Arclight marker magic value for non-used custom dimension
     */
    public static final int ARCLIGHT_DIMENSION = 0xA2c11947;

    public static int currentTick;

}
