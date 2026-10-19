package io.izzel.arclight.common.mixin.core.server.commands;

import net.minecraft.server.commands.LootCommand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.List;

@Mixin(LootCommand.class)
public abstract class LootCommandMixin {

    @ModifyVariable(method = "dropInWorld(Lnet/minecraft/commands/CommandSourceStack;Lnet/minecraft/world/phys/Vec3;Ljava/util/List;Lnet/minecraft/server/commands/LootCommand$Callback;)I",
        at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static List<ItemStack> arclight$filterEmpty(List<ItemStack> drops) {
        drops.removeIf(ItemStack::isEmpty);
        return drops;
    }
}
