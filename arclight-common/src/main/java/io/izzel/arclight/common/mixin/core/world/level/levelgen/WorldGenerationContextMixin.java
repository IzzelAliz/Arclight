package io.izzel.arclight.common.mixin.core.world.level.levelgen;

import io.izzel.arclight.common.bridge.core.world.level.levelgen.WorldGenerationContextBridge;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.Objects;

@Mixin(WorldGenerationContext.class)
public class WorldGenerationContextMixin implements WorldGenerationContextBridge {

    @Unique
    private Level arclight$level;

    @Override
    public Level bridge$getLevel() {
        return Objects.requireNonNull(this.arclight$level, "WorldGenerationContext has no level");
    }

    @Override
    public void bridge$setLevel(Level level) {
        this.arclight$level = level;
    }
}
