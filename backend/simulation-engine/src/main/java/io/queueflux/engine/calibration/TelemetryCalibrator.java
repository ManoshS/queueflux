package io.queueflux.engine.calibration;

import io.queueflux.engine.api.CalibrationRecord;
import io.queueflux.engine.api.CalibrationReport;
import io.queueflux.engine.api.SimulationEngine;
import io.queueflux.engine.api.SimulationRequest;
import io.queueflux.engine.api.SimulationResult;
import io.queueflux.engine.api.TelemetryWindow;
import io.queueflux.model.scenario.ArrivalModel;
import io.queueflux.model.scenario.QueueDefinition;
import io.queueflux.model.scenario.ScenarioDefinition;
import io.queueflux.model.scenario.SchedulingDiscipline;
import io.queueflux.model.scenario.ServiceTimeModel;
import io.queueflux.model.scenario.WorkerPoolDefinition;
import io.queueflux.model.scenario.WorkloadDefinition;
import java.util.ArrayList;
import java.util.List;

/**
 * Stateless calibration logic.  Estimates distribution parameters from a list
 * of observation windows and optionally runs a comparison simulation.
 *
 * <p>Assumptions made during fitting:
 * <ul>
 *   <li>Arrivals follow a Poisson process with the observed mean rate.
 *   <li>Service times follow an exponential distribution with the observed mean.
 *   <li>Workers are homogeneous with one concurrent slot each.
 *   <li>No retries, rate-limiting, or priority levels.
 * </ul>
 */
public class TelemetryCalibrator {
    /** Minimum simulation duration used even when the observation window is shorter. */
    private static final long MIN_SIMULATION_MICROS = 60_000_000L; // 60 s

    private final SimulationEngine engine;

    public TelemetryCalibrator(SimulationEngine engine) {
        this.engine = engine;
    }

    /**
     * Fits a model to the supplied telemetry windows, generates a suggested
     * {@link ScenarioDefinition}, and optionally runs a comparison simulation.
     *
     * @param queueId     logical name used to label the generated scenario components
     * @param windows     one or more contiguous or independent observation windows
     * @param compare     when {@code true} the engine runs the suggested scenario and
     *                    comparison metrics are included in the returned report
     * @param seed        deterministic seed for the generated scenario
     */
    public CalibrationReport calibrate(
            String queueId, List<TelemetryWindow> windows, boolean compare, long seed) {
        if (windows == null || windows.isEmpty()) {
            throw new IllegalArgumentException("at least one telemetry window is required");
        }

        long totalDurationMicros = windows.stream().mapToLong(TelemetryWindow::durationMicros).sum();
        long totalArrivals = windows.stream().mapToLong(TelemetryWindow::arrivals).sum();
        double totalDurationSeconds = totalDurationMicros / 1_000_000.0;

        double arrivalRatePerSecond = totalDurationSeconds > 0
                ? totalArrivals / totalDurationSeconds
                : 1.0;

        double meanServiceTimeMicros = estimateMeanServiceTime(windows);
        int workerCount = estimateWorkerCount(windows);

        List<String> assumptions = buildAssumptions(windows, meanServiceTimeMicros);
        CalibrationRecord record = new CalibrationRecord(
                totalDurationMicros, arrivalRatePerSecond, meanServiceTimeMicros,
                workerCount, assumptions);

        long scenarioDurationMicros = Math.max(totalDurationMicros, MIN_SIMULATION_MICROS);
        ScenarioDefinition scenario = buildScenario(
                queueId, arrivalRatePerSecond, meanServiceTimeMicros,
                workerCount, scenarioDurationMicros, seed);

        if (!compare) {
            return CalibrationReport.calibrationOnly(record, scenario);
        }

        SimulationResult result = engine.run(SimulationRequest.defaults(scenario));
        double telemetryThroughput = totalDurationSeconds > 0 ? totalArrivals / totalDurationSeconds : 0;
        return CalibrationReport.withComparison(
                record, scenario,
                telemetryThroughput,
                result.throughputPerSecond(),
                result.averageQueueWaitMicros(),
                result.p95EndToEndLatencyMicros(),
                result.p99EndToEndLatencyMicros());
    }

    private double estimateMeanServiceTime(List<TelemetryWindow> windows) {
        double sum = 0;
        long count = 0;
        for (TelemetryWindow w : windows) {
            for (double s : w.serviceTimeSamplesMicros()) {
                sum += s;
                count++;
            }
        }
        if (count == 0) {
            // No samples: use Little's law approximation if possible, else a 1-second default
            return 1_000_000.0;
        }
        return sum / count;
    }

    private int estimateWorkerCount(List<TelemetryWindow> windows) {
        double totalWorkers = windows.stream()
                .mapToInt(TelemetryWindow::observedWorkers)
                .average()
                .orElse(1);
        return Math.max(1, (int) Math.ceil(totalWorkers));
    }

    private List<String> buildAssumptions(List<TelemetryWindow> windows, double meanServiceMicros) {
        List<String> assumptions = new ArrayList<>();
        assumptions.add("Arrivals modelled as Poisson process with the observed mean rate.");
        boolean hasSamples = windows.stream().anyMatch(w -> !w.serviceTimeSamplesMicros().isEmpty());
        if (hasSamples) {
            assumptions.add("Service times modelled as exponential distribution fitted to "
                    + windows.stream().mapToLong(w -> w.serviceTimeSamplesMicros().size()).sum()
                    + " observed samples (mean " + Math.round(meanServiceMicros / 1000.0) + " ms).");
        } else {
            assumptions.add("No service-time samples provided; using 1-second default mean. "
                    + "Provide serviceTimeSamplesMicros for more accurate calibration.");
        }
        assumptions.add("Workers modelled as homogeneous with one concurrent slot each.");
        assumptions.add("No retries, rate-limiting, or priority levels included. "
                + "Add these to the suggested scenario if your system uses them.");
        assumptions.add("Calibration is an approximation. "
                + "Validate the suggested scenario against additional observation windows before relying on it.");
        return assumptions;
    }

    private ScenarioDefinition buildScenario(
            String queueId, double arrivalRate, double meanServiceMicros,
            int workerCount, long durationMicros, long seed) {
        String workerId = queueId + "-workers";
        QueueDefinition queue = new QueueDefinition(queueId, Integer.MAX_VALUE, SchedulingDiscipline.FIFO);
        WorkerPoolDefinition workers = new WorkerPoolDefinition(
                workerId, queueId, workerCount, 1,
                ServiceTimeModel.exponential(meanServiceMicros));
        WorkloadDefinition workload = new WorkloadDefinition(
                "telemetry-workload", queueId, 0, durationMicros,
                ArrivalModel.poisson(arrivalRate));
        return new ScenarioDefinition(1, "calibrated-" + queueId, durationMicros, seed,
                List.of(queue), List.of(workers), List.of(workload));
    }
}
