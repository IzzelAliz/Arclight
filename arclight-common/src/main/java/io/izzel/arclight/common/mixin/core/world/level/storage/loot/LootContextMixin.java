package io.izzel.arclight.common.mixin.core.world.level.storage.loot;

import io.izzel.arclight.common.bridge.core.world.storage.loot.LootContextBridge;
import net.minecraft.world.level.storage.loot.LootContext;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LootContext.class)
public class LootContextMixin implements LootContextBridge {
}
