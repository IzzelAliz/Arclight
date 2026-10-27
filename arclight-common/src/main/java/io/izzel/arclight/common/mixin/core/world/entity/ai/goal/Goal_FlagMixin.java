package io.izzel.arclight.common.mixin.core.world.entity.ai.goal;

import io.izzel.arclight.api.EnumHelper;
import java.util.Arrays;
import java.util.List;
import net.minecraft.world.entity.ai.goal.Goal;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Goal.Flag.class)
public class Goal_FlagMixin {
    @Shadow @Final @Mutable private static Goal.Flag[] $VALUES;

    @Inject(method = "<clinit>", at = @At("RETURN"))
    private static void arclight$unknownBehavior(CallbackInfo ci) {
        for (Goal.Flag flag : $VALUES) {
            if (flag.name().equals("UNKNOWN_BEHAVIOR")) return;
        }
        int ordinal = $VALUES.length;
        Goal.Flag flag = EnumHelper.makeEnum(Goal.Flag.class, "UNKNOWN_BEHAVIOR", ordinal, List.of(), List.of());
        $VALUES = Arrays.copyOf($VALUES, ordinal + 1);
        $VALUES[ordinal] = flag;
    }
}
