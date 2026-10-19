package io.izzel.arclight.common.bridge.core.server.level;

import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.server.bossevents.CustomBossEvents;
import net.minecraft.world.RandomSequences;
import net.minecraft.world.level.gamerules.GameRules;

/** Per-level SavedData access without changing MinecraftServer's global services. */
public interface ServerLevelSavedDataBridge {

    CustomBossEvents bridge$getCustomBossEvents();

    RandomSequences bridge$getRandomSequences();

    RandomSource bridge$getRandomSequence(Identifier key);

    GameRules bridge$getGameRules();
}
