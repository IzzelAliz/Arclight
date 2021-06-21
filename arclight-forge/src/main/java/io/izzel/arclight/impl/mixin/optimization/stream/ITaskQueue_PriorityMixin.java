package io.izzel.arclight.impl.mixin.optimization.stream;

import net.minecraft.util.concurrent.ITaskQueue;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;
import java.util.Queue;

@Mixin(ITaskQueue.Priority.class)
public class ITaskQueue_PriorityMixin {

    @Shadow @Final private List<Queue<Runnable>> queues;

    /**
     * @author IzzelAliz
     * @reason optimization
     */
    @Overwrite
    public boolean isEmpty() {
        for (Queue<Runnable> queue : this.queues) {
            if (!queue.isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
