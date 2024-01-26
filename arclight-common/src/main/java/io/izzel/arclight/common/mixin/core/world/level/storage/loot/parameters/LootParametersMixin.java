package io.izzel.arclight.common.mixin.core.world.level.storage.loot.parameters;

import io.izzel.arclight.common.mod.mixins.annotation.TransformAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LootContextParams.class)
public class LootParametersMixin {

    @TransformAccess(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL)
    private static final LootContextParam<Integer> LOOTING_MOD = new LootContextParam<>(new ResourceLocation("bukkit:looting_mod"));
}
