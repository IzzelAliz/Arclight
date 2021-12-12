package io.izzel.arclight.common.mod.util.optimization.moveinterp;

public interface TickPredictor {

    double tick();

    class AvgTick implements TickPredictor {

        private static final double FACTOR = 0.5;

        private long lastTick;
        private double avgTick = 50;

        @Override
        public double tick() {
            if (lastTick == 0) {
                lastTick = System.currentTimeMillis();
            } else {
                var elapsed = (System.currentTimeMillis() - lastTick) / 20d;
                lastTick = System.currentTimeMillis();
                if (elapsed > 0) {
                    avgTick = (avgTick * FACTOR) + (elapsed * (1 - FACTOR));
                }
            }
            return avgTick;
        }
    }
}
