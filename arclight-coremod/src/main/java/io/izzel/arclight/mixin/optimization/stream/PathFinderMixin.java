package io.izzel.arclight.mixin.optimization.stream;

import net.minecraft.pathfinding.FlaggedPathPoint;
import net.minecraft.pathfinding.NodeProcessor;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathFinder;
import net.minecraft.pathfinding.PathHeap;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Mixin(PathFinder.class)
public abstract class PathFinderMixin {

    // @formatter:off
    @Shadow protected abstract float func_224776_a(PathPoint p_224776_1_, Set<FlaggedPathPoint> p_224776_2_);
    @Shadow @Final private PathHeap path;
    @Shadow @Final private Set<PathPoint> closedSet;
    @Shadow @Final private int field_215751_d;
    @Shadow private NodeProcessor nodeProcessor;
    @Shadow @Final private PathPoint[] pathOptions;
    @Shadow protected abstract Path func_224780_a(PathPoint p_224780_1_, BlockPos p_224780_2_, boolean p_224780_3_);
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    @Nullable
    private Path func_224779_a(PathPoint p_224779_1_, Map<FlaggedPathPoint, BlockPos> p_224779_2_, float p_224779_3_, int p_224779_4_) {
        Set<FlaggedPathPoint> set = p_224779_2_.keySet();
        p_224779_1_.totalPathDistance = 0.0F;
        p_224779_1_.distanceToNext = this.func_224776_a(p_224779_1_, set);
        p_224779_1_.distanceToTarget = p_224779_1_.distanceToNext;
        this.path.clearPath();
        this.closedSet.clear();
        this.path.addPoint(p_224779_1_);
        int i = 0;

        while (!this.path.isPathEmpty()) {
            ++i;
            if (i >= this.field_215751_d) {
                break;
            }

            PathPoint pathpoint = this.path.dequeue();
            pathpoint.visited = true;
            for (FlaggedPathPoint p_224781_2_ : set) {
                if (pathpoint.func_224757_c(p_224781_2_) <= (float) p_224779_4_) {
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

            if (!(pathpoint.distanceTo(p_224779_1_) >= p_224779_3_)) {
                int j = this.nodeProcessor.func_222859_a(this.pathOptions, pathpoint);

                for (int k = 0; k < j; ++k) {
                    PathPoint pathpoint1 = this.pathOptions[k];
                    float f = pathpoint.distanceTo(pathpoint1);
                    pathpoint1.field_222861_j = pathpoint.field_222861_j + f;
                    float f1 = pathpoint.totalPathDistance + f + pathpoint1.costMalus;
                    if (pathpoint1.field_222861_j < p_224779_3_ && (!pathpoint1.isAssigned() || f1 < pathpoint1.totalPathDistance)) {
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

        List<Path> candidates = new ArrayList<>();
        boolean b = false;
        for (FlaggedPathPoint flaggedPathPoint : set) {
            if (flaggedPathPoint.func_224762_f()) {
                b = true;
                break;
            }
        }
        if (b) {
            for (FlaggedPathPoint point : set) {
                if (point.func_224762_f()) {
                    Path path = this.func_224780_a(point.func_224763_d(), p_224779_2_.get(point), true);
                    candidates.add(path);
                }
            }
            if (candidates.isEmpty()) return null;
            candidates.sort(Comparator.comparingInt(Path::getCurrentPathLength));
        } else {
            for (FlaggedPathPoint point : set) {
                Path path = this.func_224780_a(point.func_224763_d(), p_224779_2_.get(point), false);
                candidates.add(path);
            }
            if (candidates.isEmpty()) return null;
            candidates.sort(Comparator.comparingDouble(Path::func_224769_l).thenComparingInt(Path::getCurrentPathLength));
        }

        return candidates.get(0);
    }
}
