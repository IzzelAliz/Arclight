package io.izzel.arclight.common.mod.util;

/** Apply CraftBukkit's extended-wave curve only to its five defined raider types. */
public final class RaidWaveSpawns {

    private RaidWaveSpawns() {
    }

    public static int count(Enum<?> type, int[] counts, int wave) {
        int divisor = switch (type.name()) {
            case "PILLAGER" -> 2;
            case "VINDICATOR" -> 3;
            case "EVOKER" -> 4;
            case "WITCH" -> 5;
            case "RAVAGER" -> 6;
            default -> 0;
        };
        // CB specifies no curve for NeoForge extensions. Retain the native array read,
        // including its bounds behavior, rather than selecting an unapproved fallback.
        if (divisor == 0) {
            return counts[wave];
        }
        if (wave < 0) {
            return 0;
        }
        if (wave < counts.length) {
            return counts[wave];
        }
        int lastHardcodedWave = counts.length - 1;
        int lastHardcodedCount = counts[lastHardcodedWave];
        int extraWaves = wave - lastHardcodedWave;
        return lastHardcodedCount + extraWaves / divisor;
    }
}
