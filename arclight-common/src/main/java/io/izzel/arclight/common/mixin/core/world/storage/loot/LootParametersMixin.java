package io.izzel.arclight.common.mixin.core.world.storage.loot;

import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.loot.LootParameter;
import net.minecraft.world.storage.loot.LootParameters;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LootParameters.class)
public class LootParametersMixin {

    private static final LootParameter<Integer> LOOTING_MOD = new LootParameter<>(new ResourceLocation("bukkit:looting_mod"));;
}
