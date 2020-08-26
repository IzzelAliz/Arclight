package io.izzel.arclight.common.mixin.core.loot;

import io.izzel.arclight.common.mod.ArclightConstants;
import net.minecraft.loot.LootParameter;
import net.minecraft.loot.LootParameters;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LootParameters.class)
public class LootParametersMixin {

    private static final LootParameter<Integer> LOOTING_MOD = ArclightConstants.LOOTING_MOD;
}
