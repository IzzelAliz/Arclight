package io.izzel.arclight.boot.neoforge.mod;

import net.neoforged.neoforgespi.transformation.ClassProcessorProvider;

public class ArclightClassProcessorProvider implements ClassProcessorProvider {

    @Override
    public void createProcessors(Context context, Collector collector) {
        collector.add(new ArclightClassProcessor());
    }
}
