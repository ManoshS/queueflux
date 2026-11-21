package io.queueflux.engine.api;

import java.util.List;

/**
 * One contiguous observation window collected from a live queue.
 * Service-time samples are in microseconds; counts are raw job counts.
 */
public record TelemetryWindow(
        long startMicros,
        long endMicros,
        long arrivals,
        List<Double> serviceTimeSamplesMicros,
        int observedWorkers) {
    public TelemetryWindow {
        if (endMicros <= startMicros) throw new IllegalArgumentException("endMicros must be > startMicros");
        if (arrivals < 0) throw new IllegalArgumentException("arrivals must be >= 0");
        if (observedWorkers < 1) throw new IllegalArgumentException("observedWorkers must be >= 1");
        for (Double s : serviceTimeSamplesMicros) {
            if (s == null || !Double.isFinite(s) || s <= 0) {
                throw new IllegalArgumentException("every service-time sample must be finite and > 0");
            }
        }
        serviceTimeSamplesMicros = List.copyOf(serviceTimeSamplesMicros);
    }

    public TelemetryWindow(long startMicros, long endMicros, long arrivals, int observedWorkers) {
        this(startMicros, endMicros, arrivals, List.of(), observedWorkers);
    }

    public long durationMicros() {
        return endMicros - startMicros;
    }
}
