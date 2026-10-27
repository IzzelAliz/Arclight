package io.izzel.arclight.common.mixin.core.world.entity.animal;

import io.izzel.arclight.common.bridge.core.world.entity.animal.SchoolingFishBridge;
import net.minecraft.world.entity.animal.fish.AbstractSchoolingFish;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(AbstractSchoolingFish.class)
public abstract class SchoolingFishMixin implements SchoolingFishBridge {

    @Shadow public int schoolSize;
    @Shadow @Nullable public AbstractSchoolingFish leader;

    @Override
    public int bridge$getSchoolSize() {
        return this.schoolSize;
    }

    @Override
    @Nullable
    public AbstractSchoolingFish bridge$getSchoolLeader() {
        return this.leader;
    }
}
