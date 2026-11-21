package io.queueflux.server.api;

import io.queueflux.model.scenario.ScenarioDefinition;

public record ComparisonRequest(ScenarioDefinition scenario, int replications, long seedStride, long warmUpMicros) {
    public ComparisonRequest(ScenarioDefinition scenario, int replications, long seedStride) {
        this(scenario, replications, seedStride, 0);
    }

    public void validate() {
        if (scenario == null) throw new IllegalArgumentException("scenario is required");
        if (replications < 2 || replications > 30) throw new IllegalArgumentException("replications must be between 2 and 30");
        if (seedStride < 1) throw new IllegalArgumentException("seedStride must be >= 1");
        if (warmUpMicros < 0) throw new IllegalArgumentException("warmUpMicros must be >= 0");
        if (warmUpMicros >= scenario.durationMicros()) throw new IllegalArgumentException("warmUpMicros must be less than durationMicros");
    }
}
