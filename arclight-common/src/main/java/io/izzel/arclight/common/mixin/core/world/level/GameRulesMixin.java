package io.izzel.arclight.common.mixin.core.world.level;

import io.izzel.arclight.common.bridge.core.world.level.GameRulesBridge;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleMap;
import net.minecraft.world.level.gamerules.GameRules;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Set;

@Mixin(GameRules.class)
public abstract class GameRulesMixin implements GameRulesBridge {

    @Shadow
    @Final
    private GameRuleMap rules;

    @Override
    public Set<GameRule<?>> arclight$getAllRules() {
        return rules.keySet();
    }
}
