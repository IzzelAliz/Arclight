package io.izzel.arclight.common.mixin.core.loot;

import net.minecraft.loot.LootParameter;
import net.minecraft.loot.LootParameters;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LootParameters.class)
public class LootParametersMixin {

    private static final LootParameter<Integer> LOOTING_MOD = new LootParameter<>(new ResourceLocation("bukkit:looting_mod"));
}
