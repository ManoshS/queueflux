package io.queueflux.engine.api;

import java.util.List;

/**
 * Parameters estimated from telemetry windows, together with the assumptions
 * that were made when fitting the model.
 */
public record CalibrationRecord(
        long observationWindowMicros,
        double observedArrivalRatePerSecond,
        double estimatedMeanServiceTimeMicros,
        int suggestedWorkerCount,
        List<String> assumptions) {
    public CalibrationRecord {
        assumptions = List.copyOf(assumptions);
    }
}
