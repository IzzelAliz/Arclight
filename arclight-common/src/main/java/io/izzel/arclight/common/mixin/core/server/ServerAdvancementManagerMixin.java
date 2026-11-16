package io.izzel.arclight.common.mixin.core.server;

import io.izzel.arclight.mixin.Decorate;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.ServerAdvancementManager;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.HashMap;
import java.util.Map;

@Mixin(ServerAdvancementManager.class)
public class ServerAdvancementManagerMixin {

    @Shadow @Final @Mutable private Map<Identifier, AdvancementHolder> advancements;

    @Decorate(inject = true, method = {"<init>(Lnet/minecraft/core/HolderLookup$Provider;)V", "<init>(Lnet/minecraft/core/HolderLookup$Provider;Lnet/minecraftforge/common/crafting/conditions/ICondition$IContext;)V"}, at = @At(value = "FIELD", shift = At.Shift.AFTER, opcode = Opcodes.PUTFIELD, target = "Lnet/minecraft/server/ServerAdvancementManager;advancements:Ljava/util/Map;"))
    private void arclight$buildMutable() {
        this.advancements = new HashMap<>(this.advancements);
    }
}
