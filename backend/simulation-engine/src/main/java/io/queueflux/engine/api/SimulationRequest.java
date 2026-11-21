package io.queueflux.engine.api;

import io.queueflux.model.scenario.ScenarioDefinition;

public record SimulationRequest(ScenarioDefinition scenario, int maxGeneratedJobs, int maxEvents, int maxTraceEvents, long warmUpMicros) {
    public SimulationRequest {
        if (scenario == null) throw new IllegalArgumentException("scenario is required");
        if (maxGeneratedJobs < 1 || maxEvents < 1 || maxTraceEvents < 0) {
            throw new IllegalArgumentException("simulation limits are invalid");
        }
        if (warmUpMicros < 0) throw new IllegalArgumentException("warmUpMicros must be >= 0");
        if (warmUpMicros >= scenario.durationMicros()) throw new IllegalArgumentException("warmUpMicros must be less than durationMicros");
    }

    public static SimulationRequest defaults(ScenarioDefinition scenario) {
        return new SimulationRequest(scenario, 1_000_000, 5_000_000, 100_000, 0);
    }

    public static SimulationRequest withWarmUp(ScenarioDefinition scenario, long warmUpMicros) {
        return new SimulationRequest(scenario, 1_000_000, 5_000_000, 100_000, warmUpMicros);
    }
}
