package io.queueflux.engine.api;

import java.util.List;

public record SimulationResult(
        String scenarioName,
        long seed,
        long simulationDurationMicros,
        long warmUpMicros,
        long processedEvents,
        long generatedJobs,
        long acceptedJobs,
        long completedJobs,
        long rejectedJobs,
        long retriedJobs,
        long failedAttempts,
        long deadLetteredJobs,
        long waitingAtEnd,
        long delayedRetryAtEnd,
        long activeAtEnd,
        long peakBacklog,
        double throughputPerSecond,
        double averageQueueWaitMicros,
        double averageEndToEndLatencyMicros,
        double p95EndToEndLatencyMicros,
        double p99EndToEndLatencyMicros,
        double workerUtilization,
        // Post-warm-up steady-state metrics (same as above when warmUpMicros == 0)
        long postWarmUpCompletedJobs,
        double postWarmUpThroughputPerSecond,
        double postWarmUpAverageWaitMicros,
        double postWarmUpP95LatencyMicros,
        double postWarmUpP99LatencyMicros,
        List<QueueMetrics> queues,
        List<TraceEvent> trace) {
    public SimulationResult {
        queues = List.copyOf(queues);
        trace = List.copyOf(trace);
    }
}
