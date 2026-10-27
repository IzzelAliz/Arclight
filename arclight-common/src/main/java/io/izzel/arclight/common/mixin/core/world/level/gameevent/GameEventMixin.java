package io.izzel.arclight.common.mixin.core.world.level.gameevent;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.papermc.paper.registry.PaperRegistryListenerManager;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.gameevent.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GameEvent.class)
public class GameEventMixin {
    @WrapOperation(method = "register(Ljava/lang/String;I)Lnet/minecraft/core/Holder$Reference;", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/Registry;registerForHolder(Lnet/minecraft/core/Registry;Lnet/minecraft/resources/Identifier;Ljava/lang/Object;)Lnet/minecraft/core/Holder$Reference;"))
    private static Holder.Reference<GameEvent> arclight$register(Registry<GameEvent> registry, Identifier key, Object value,
            Operation<Holder.Reference<GameEvent>> original) {
        return PaperRegistryListenerManager.INSTANCE.registerForHolderWithListeners(registry, key, (GameEvent) value);
    }
}
