package io.izzel.arclight.common.mixin.core.world.level;

import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.*;

@Mixin(ChunkPos.class)
public class ChunkPosMixin {
    @Final
    @Shadow
    public int x;
    @Shadow
    @Final
    public int z;

    @Unique
    private Long arclight$cachedToLong;

    /**
     * @author Goodvise
     * @reason optimization
     */
    @Overwrite
    public long toLong() {
        if (arclight$cachedToLong == null) {
            arclight$cachedToLong = ChunkPos.asLong(x, z);
        }

        return arclight$cachedToLong;
    }
}
