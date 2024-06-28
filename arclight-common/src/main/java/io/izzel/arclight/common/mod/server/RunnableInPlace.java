package io.izzel.arclight.common.mod.server;

public interface RunnableInPlace extends Runnable {

    static RunnableInPlace wrap(Runnable r) {
        if (r instanceof RunnableInPlace i) {
            return i;
        } else {
            return r::run;
        }
    }
}
