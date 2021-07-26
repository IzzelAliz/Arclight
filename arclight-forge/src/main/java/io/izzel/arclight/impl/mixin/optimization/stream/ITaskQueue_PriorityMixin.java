package io.izzel.arclight.impl.mixin.optimization.stream;

import net.minecraft.util.thread.StrictQueue;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;
import java.util.Queue;

@Mixin(StrictQueue.FixedPriorityQueue.class)
public class ITaskQueue_PriorityMixin {

    @Shadow @Final private List<Queue<Runnable>> queueList;

    /**
     * @author IzzelAliz
     * @reason optimization
     */
    @Overwrite
    public boolean isEmpty() {
        for (Queue<Runnable> queue : this.queueList) {
            if (!queue.isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
