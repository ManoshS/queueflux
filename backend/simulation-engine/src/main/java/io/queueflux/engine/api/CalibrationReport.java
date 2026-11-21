package io.queueflux.engine.api;

import io.queueflux.model.scenario.ScenarioDefinition;

/**
 * Result of a calibration request.  The comparison section is populated only
 * when the caller asked for a simulation run alongside the calibration.
 *
 * <p>Fit quality thresholds (throughput deviation):
 * <ul>
 *   <li>GOOD — deviation &lt; 5 %
 *   <li>ACCEPTABLE — deviation &lt; 20 %
 *   <li>POOR — deviation ≥ 20 %
 * </ul>
 */
public record CalibrationReport(
        CalibrationRecord calibration,
        ScenarioDefinition suggestedScenario,
        Double telemetryThroughputPerSecond,
        Double simulationThroughputPerSecond,
        Double simulationAverageWaitMicros,
        Double simulationP95LatencyMicros,
        Double simulationP99LatencyMicros,
        Double throughputDeviationPercent,
        String fitQuality) {

    /** Builds a calibration-only report without any simulation comparison. */
    public static CalibrationReport calibrationOnly(CalibrationRecord calibration, ScenarioDefinition scenario) {
        return new CalibrationReport(calibration, scenario, null, null, null, null, null, null, null);
    }

    /** Builds a report that includes both telemetry and simulation comparison data. */
    public static CalibrationReport withComparison(
            CalibrationRecord calibration,
            ScenarioDefinition scenario,
            double telemetryThroughput,
            double simulationThroughput,
            double simulationAverageWait,
            double simulationP95,
            double simulationP99) {
        double deviation = telemetryThroughput > 0
                ? Math.abs(simulationThroughput - telemetryThroughput) / telemetryThroughput * 100.0
                : 0.0;
        String quality = deviation < 5.0 ? "GOOD" : deviation < 20.0 ? "ACCEPTABLE" : "POOR";
        return new CalibrationReport(calibration, scenario, telemetryThroughput,
                simulationThroughput, simulationAverageWait, simulationP95, simulationP99,
                deviation, quality);
    }
}
