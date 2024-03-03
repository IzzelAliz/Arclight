package io.izzel.arclight.common.mixin.core.world.level;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(GameRules.Value.class)
public abstract class GameRules_ValueMixin {

    // @formatter:off
    @Shadow public abstract void onChanged(@Nullable MinecraftServer minecraftServer);
    // @formatter:on

    public void onChanged(ServerLevel level) {
        this.onChanged(level.getServer());
    }
}
