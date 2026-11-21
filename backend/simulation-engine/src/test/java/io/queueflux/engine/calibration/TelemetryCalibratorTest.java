package io.queueflux.engine.calibration;

import io.queueflux.engine.DefaultSimulationEngine;
import io.queueflux.engine.api.CalibrationReport;
import io.queueflux.engine.api.TelemetryWindow;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TelemetryCalibratorTest {

    private final TelemetryCalibrator calibrator = new TelemetryCalibrator(new DefaultSimulationEngine());

    /** 60-second window, 300 arrivals → 5 jobs/s, 3 workers, 50 ms service time samples. */
    private static TelemetryWindow window60s() {
        List<Double> samples = List.of(50_000.0, 48_000.0, 52_000.0, 49_000.0, 51_000.0);
        return new TelemetryWindow(0, 60_000_000, 300, samples, 3);
    }

    @Test void calibrationOnlyDoesNotRunSimulation() {
        CalibrationReport report = calibrator.calibrate("orders", List.of(window60s()), false, 1L);

        assertThat(report.calibration().observedArrivalRatePerSecond()).isCloseTo(5.0, org.assertj.core.data.Offset.offset(0.01));
        assertThat(report.calibration().estimatedMeanServiceTimeMicros()).isCloseTo(50_000.0, org.assertj.core.data.Offset.offset(1_000.0));
        assertThat(report.calibration().suggestedWorkerCount()).isEqualTo(3);
        assertThat(report.suggestedScenario().name()).isEqualTo("calibrated-orders");
        assertThat(report.suggestedScenario().seed()).isEqualTo(1L);

        // Comparison section must be absent
        assertThat(report.simulationThroughputPerSecond()).isNull();
        assertThat(report.fitQuality()).isNull();
    }

    @Test void comparisonRunsFitScenarioAndProducesDeviationMetrics() {
        CalibrationReport report = calibrator.calibrate("orders", List.of(window60s()), true, 42L);

        assertThat(report.simulationThroughputPerSecond()).isNotNull().isPositive();
        assertThat(report.simulationP95LatencyMicros()).isNotNull().isPositive();
        assertThat(report.throughputDeviationPercent()).isNotNull().isGreaterThanOrEqualTo(0.0);
        assertThat(report.fitQuality()).isIn("GOOD", "ACCEPTABLE", "POOR");
    }

    @Test void isSeedDeterministic() {
        CalibrationReport r1 = calibrator.calibrate("q", List.of(window60s()), true, 7L);
        CalibrationReport r2 = calibrator.calibrate("q", List.of(window60s()), true, 7L);
        assertThat(r1.simulationThroughputPerSecond()).isEqualTo(r2.simulationThroughputPerSecond());
        assertThat(r1.simulationP95LatencyMicros()).isEqualTo(r2.simulationP95LatencyMicros());
    }

    @Test void multipleWindowsAggregateArrivalsCorrectly() {
        // Two non-overlapping 30-second windows, each with 150 arrivals → still 5 jobs/s overall
        TelemetryWindow w1 = new TelemetryWindow(0, 30_000_000, 150, List.of(50_000.0), 2);
        TelemetryWindow w2 = new TelemetryWindow(30_000_000, 60_000_000, 150, List.of(50_000.0), 2);
        CalibrationReport report = calibrator.calibrate("mail", List.of(w1, w2), false, 1L);
        assertThat(report.calibration().observedArrivalRatePerSecond()).isCloseTo(5.0, org.assertj.core.data.Offset.offset(0.01));
    }

    @Test void windowsWithoutServiceSamplesUseFallbackMean() {
        TelemetryWindow noSamples = new TelemetryWindow(0, 60_000_000, 300, 2);
        CalibrationReport report = calibrator.calibrate("q", List.of(noSamples), false, 1L);
        assertThat(report.calibration().estimatedMeanServiceTimeMicros()).isEqualTo(1_000_000.0);
        assertThat(report.calibration().assumptions()).anyMatch(a -> a.contains("No service-time samples"));
    }

    @Test void assumptionsAlwaysIncludeApproximationWarning() {
        CalibrationReport report = calibrator.calibrate("q", List.of(window60s()), false, 1L);
        assertThat(report.calibration().assumptions()).anyMatch(a -> a.contains("approximation"));
    }

    @Test void rejectsNullWindowList() {
        assertThatThrownBy(() -> calibrator.calibrate("q", null, false, 1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test void rejectsEmptyWindowList() {
        assertThatThrownBy(() -> calibrator.calibrate("q", List.of(), false, 1L))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
