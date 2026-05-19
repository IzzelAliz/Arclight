package io.izzel.arclight.common.mixin.core.world.level.block;

import net.minecraft.world.level.block.LightBlock;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LightBlock.class)
public class LightBlockMixin {

    /**
     * @author IzzelAliz
     * @reason Avoid resolving Items.LIGHT while Blocks is still initializing. LightBlock#getShape checks
     * the held light item through CollisionContext, so caching the shape during Blocks.<clinit> can recurse
     * into Items.<clinit> before item registration is ready.
     */
    public boolean method_9543() {
        return true;
    }
}
