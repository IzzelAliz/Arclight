package io.izzel.arclight.impl.mixin.v1_15.optimization.stream;

import net.minecraft.entity.MobEntity;
import net.minecraft.pathfinding.FlaggedPathPoint;
import net.minecraft.pathfinding.NodeProcessor;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathFinder;
import net.minecraft.pathfinding.PathHeap;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Region;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Mixin(PathFinder.class)
public abstract class PathFinderMixin {

    // @formatter:off
    @Shadow protected abstract float func_224776_a(PathPoint p_224776_1_, Set<FlaggedPathPoint> p_224776_2_);
    @Shadow @Final private PathHeap path;
    @Shadow @Final private Set<PathPoint> closedSet;
    @Shadow @Final private int field_215751_d;
    @Shadow @Final private NodeProcessor nodeProcessor;
    @Shadow @Final private PathPoint[] pathOptions;
    @Shadow protected abstract Path func_224780_a(PathPoint p_224780_1_, BlockPos p_224780_2_, boolean p_224780_3_);
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    @Nullable
    public Path func_227478_a_(Region p_227478_1_, MobEntity p_227478_2_, Set<BlockPos> p_227478_3_, float p_227478_4_, int p_227478_5_, float p_227478_6_) {
        this.path.clearPath();
        this.nodeProcessor.func_225578_a_(p_227478_1_, p_227478_2_);
        PathPoint pathpoint = this.nodeProcessor.getStart();
        Map<FlaggedPathPoint, BlockPos> map = new HashMap<>();
        for (BlockPos p_224782_1_ : p_227478_3_) {
            if (map.put(this.nodeProcessor.func_224768_a(p_224782_1_.getX(), p_224782_1_.getY(), p_224782_1_.getZ()), p_224782_1_) != null) {
                throw new IllegalStateException("Duplicate key");
            }
        }
        Path path = this.func_227479_a_(pathpoint, map, p_227478_4_, p_227478_5_, p_227478_6_);
        this.nodeProcessor.postProcess();
        return path;
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    @Nullable
    private Path func_227479_a_(PathPoint p_227479_1_, Map<FlaggedPathPoint, BlockPos> p_227479_2_, float p_227479_3_, int p_227479_4_, float p_227479_5_) {
        Set<FlaggedPathPoint> set = p_227479_2_.keySet();
        p_227479_1_.totalPathDistance = 0.0F;
        p_227479_1_.distanceToNext = this.func_224776_a(p_227479_1_, set);
        p_227479_1_.distanceToTarget = p_227479_1_.distanceToNext;
        this.path.clearPath();
        this.closedSet.clear();
        this.path.addPoint(p_227479_1_);
        int i = 0;
        int j = (int) ((float) this.field_215751_d * p_227479_5_);

        while (!this.path.isPathEmpty()) {
            ++i;
            if (i >= j) {
                break;
            }

            PathPoint pathpoint = this.path.dequeue();
            pathpoint.visited = true;
            for (FlaggedPathPoint p_224781_2_ : set) {
                if (pathpoint.func_224757_c(p_224781_2_) <= (float) p_227479_4_) {
                    p_224781_2_.func_224764_e();
                }
            }
            boolean b = false;
            for (FlaggedPathPoint flaggedPathPoint : set) {
                if (flaggedPathPoint.func_224762_f()) {
                    b = true;
                    break;
                }
            }
            if (b) {
                break;
            }

            if (!(pathpoint.distanceTo(p_227479_1_) >= p_227479_3_)) {
                int k = this.nodeProcessor.func_222859_a(this.pathOptions, pathpoint);

                for (int l = 0; l < k; ++l) {
                    PathPoint pathpoint1 = this.pathOptions[l];
                    float f = pathpoint.distanceTo(pathpoint1);
                    pathpoint1.field_222861_j = pathpoint.field_222861_j + f;
                    float f1 = pathpoint.totalPathDistance + f + pathpoint1.costMalus;
                    if (pathpoint1.field_222861_j < p_227479_3_ && (!pathpoint1.isAssigned() || f1 < pathpoint1.totalPathDistance)) {
                        pathpoint1.previous = pathpoint;
                        pathpoint1.totalPathDistance = f1;
                        pathpoint1.distanceToNext = this.func_224776_a(pathpoint1, set) * 1.5F;
                        if (pathpoint1.isAssigned()) {
                            this.path.changeDistance(pathpoint1, pathpoint1.totalPathDistance + pathpoint1.distanceToNext);
                        } else {
                            pathpoint1.distanceToTarget = pathpoint1.totalPathDistance + pathpoint1.distanceToNext;
                            this.path.addPoint(pathpoint1);
                        }
                    }
                }
            }
        }

        Optional<Path> optional;
        if (set.stream().anyMatch(FlaggedPathPoint::func_224762_f)) {
            List<Path> toSort = new ArrayList<>();
            for (FlaggedPathPoint p_224778_2_ : set) {
                if (p_224778_2_.func_224762_f()) {
                    Path func_224780_a = this.func_224780_a(p_224778_2_.func_224763_d(), p_227479_2_.get(p_224778_2_), true);
                    toSort.add(func_224780_a);
                }
            }
            toSort.sort(Comparator.comparingInt(Path::getCurrentPathLength));
            Optional<Path> found = Optional.empty();
            for (Path func_224780_a : toSort) {
                found = Optional.of(func_224780_a);
                break;
            }
            optional = found;
        } else {
            List<Path> toSort = new ArrayList<>();
            for (FlaggedPathPoint p_224777_2_ : set) {
                Path func_224780_a = this.func_224780_a(p_224777_2_.func_224763_d(), p_227479_2_.get(p_224777_2_), false);
                toSort.add(func_224780_a);
            }
            toSort.sort(Comparator.comparingDouble(Path::func_224769_l).thenComparingInt(Path::getCurrentPathLength));
            Optional<Path> found = Optional.empty();
            for (Path func_224780_a : toSort) {
                found = Optional.of(func_224780_a);
                break;
            }
            optional = found;
        }

        if (!optional.isPresent()) {
            return null;
        } else {
            Path path = optional.get();
            return path;
        }
    }
}
