package io.izzel.arclight.common.mod.util;

import net.minecraft.core.Holder;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import org.bukkit.TreeType;
import org.jspecify.annotations.Nullable;

/** Tree selection produced during natural growth or a bone-meal capture. */
public final class TreeGrowthContext {
    private TreeGrowthContext() {}
    public enum Kind { EMPTY, KNOWN, UNKNOWN }
    public enum Site { MEGA, ORDINARY, FUNGUS }
    public record Selection(Site site, Holder<ConfiguredFeature<?, ?>> holder,
                            @Nullable ResourceKey<ConfiguredFeature<?, ?>> key, @Nullable TreeType type) {}
    public record ObservedMushroom(Object block, Holder<ConfiguredFeature<?, ?>> holder) {}
    public record Result(ServerLevel level, Object invocation, Kind kind,
                         @Nullable Selection selection, boolean returnedNormally,
                         java.util.List<ObservedMushroom> observedMushrooms) {}

    private static Scope observer;
    private static Frame current;

    public static Scope observe(ServerLevel level) {
        return new Scope(level);
    }

    public static Scope observeCapture(ServerLevel level) {
        return new Scope(level);
    }

    public static final class Scope implements AutoCloseable {
        private final ServerLevel level;
        private Result result;

        private Scope(ServerLevel level) {
            this.level = level;
            observer = this;
        }

        public @Nullable Result result() {
            return result;
        }

        @Override
        public void close() {
            observer = null;
        }
    }

    public static Frame enter(ServerLevel level) {
        current = new Frame(level);
        return current;
    }

    public static final class Frame implements AutoCloseable {
        private final ServerLevel level;
        private Selection selection;
        private final java.util.List<ObservedMushroom> mushrooms = new java.util.ArrayList<>();
        private boolean returnedNormally;
        private boolean megaSeen;

        private Frame(ServerLevel level) {
            this.level = level;
        }

        public void returnedNormally() {
            returnedNormally = true;
        }

        @Override
        public void close() {
            if (observer != null && observer.level == level) {
                observer.result = new Result(level, this,
                    selection == null ? Kind.EMPTY : selection.type() == null ? Kind.UNKNOWN : Kind.KNOWN,
                    selection, returnedNormally, java.util.List.copyOf(mushrooms));
            }
            current = null;
        }
    }

    public static void observedMushroom(ServerLevel level, Object block, Holder<ConfiguredFeature<?, ?>> holder) {
        if (current != null && current.level == level) {
            current.mushrooms.add(new ObservedMushroom(block, holder));
        }
    }

    public static void selected(ServerLevel level, Site site, Holder<ConfiguredFeature<?, ?>> holder) {
        if (current == null || current.level != level) return;
        if (site == Site.MEGA && current.megaSeen) return;
        if (site == Site.MEGA) current.megaSeen = true;
        ResourceKey<ConfiguredFeature<?, ?>> key = holder.unwrapKey().orElse(null);
        current.selection = new Selection(site, holder, key, knownType(key));
    }

    public static @Nullable TreeType knownType(@Nullable ResourceKey<ConfiguredFeature<?, ?>> worldgentreeabstract) {
        if (worldgentreeabstract == TreeFeatures.WARPED_FUNGUS || worldgentreeabstract == TreeFeatures.WARPED_FUNGUS_PLANTED) {
            return TreeType.WARPED_FUNGUS;
        } else if (worldgentreeabstract == TreeFeatures.CRIMSON_FUNGUS || worldgentreeabstract == TreeFeatures.CRIMSON_FUNGUS_PLANTED) {
            return TreeType.CRIMSON_FUNGUS;
        } else if (worldgentreeabstract == TreeFeatures.OAK || worldgentreeabstract == TreeFeatures.OAK_BEES_005) {
            return TreeType.TREE;
        } else if (worldgentreeabstract == TreeFeatures.HUGE_RED_MUSHROOM) {
            return TreeType.RED_MUSHROOM;
        } else if (worldgentreeabstract == TreeFeatures.HUGE_BROWN_MUSHROOM) {
            return TreeType.BROWN_MUSHROOM;
        } else if (worldgentreeabstract == TreeFeatures.JUNGLE_TREE) {
            return TreeType.COCOA_TREE;
        } else if (worldgentreeabstract == TreeFeatures.JUNGLE_TREE_NO_VINE) {
            return TreeType.SMALL_JUNGLE;
        } else if (worldgentreeabstract == TreeFeatures.PINE) {
            return TreeType.TALL_REDWOOD;
        } else if (worldgentreeabstract == TreeFeatures.SPRUCE) {
            return TreeType.REDWOOD;
        } else if (worldgentreeabstract == TreeFeatures.ACACIA) {
            return TreeType.ACACIA;
        } else if (worldgentreeabstract == TreeFeatures.BIRCH || worldgentreeabstract == TreeFeatures.BIRCH_BEES_005) {
            return TreeType.BIRCH;
        } else if (worldgentreeabstract == TreeFeatures.SUPER_BIRCH_BEES_0002) {
            return TreeType.TALL_BIRCH;
        } else if (worldgentreeabstract == TreeFeatures.SWAMP_OAK) {
            return TreeType.SWAMP;
        } else if (worldgentreeabstract == TreeFeatures.FANCY_OAK || worldgentreeabstract == TreeFeatures.FANCY_OAK_BEES_005) {
            return TreeType.BIG_TREE;
        } else if (worldgentreeabstract == TreeFeatures.JUNGLE_BUSH) {
            return TreeType.JUNGLE_BUSH;
        } else if (worldgentreeabstract == TreeFeatures.DARK_OAK) {
            return TreeType.DARK_OAK;
        } else if (worldgentreeabstract == TreeFeatures.MEGA_SPRUCE) {
            return TreeType.MEGA_REDWOOD;
        } else if (worldgentreeabstract == TreeFeatures.MEGA_PINE) {
            return TreeType.MEGA_PINE;
        } else if (worldgentreeabstract == TreeFeatures.MEGA_JUNGLE_TREE) {
            return TreeType.JUNGLE;
        } else if (worldgentreeabstract == TreeFeatures.AZALEA_TREE) {
            return TreeType.AZALEA;
        } else if (worldgentreeabstract == TreeFeatures.MANGROVE) {
            return TreeType.MANGROVE;
        } else if (worldgentreeabstract == TreeFeatures.TALL_MANGROVE) {
            return TreeType.TALL_MANGROVE;
        } else if (worldgentreeabstract == TreeFeatures.CHERRY || worldgentreeabstract == TreeFeatures.CHERRY_BEES_005) {
            return TreeType.CHERRY;
        } else if (worldgentreeabstract == TreeFeatures.PALE_OAK || worldgentreeabstract == TreeFeatures.PALE_OAK_BONEMEAL) {
            return TreeType.PALE_OAK;
        } else if (worldgentreeabstract == TreeFeatures.PALE_OAK_CREAKING) {
            return TreeType.PALE_OAK_CREAKING;
        } else {
            return null;
        }

    }
}
