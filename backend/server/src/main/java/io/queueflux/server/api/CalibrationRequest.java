package io.queueflux.server.api;

import io.queueflux.engine.api.TelemetryWindow;
import java.util.List;

public record CalibrationRequest(
        String queueId,
        List<TelemetryWindow> windows,
        boolean compare,
        long seed) {

    public CalibrationRequest {
        if (queueId == null || queueId.isBlank()) throw new IllegalArgumentException("queueId is required");
        if (windows == null || windows.isEmpty()) throw new IllegalArgumentException("at least one window is required");
        windows = List.copyOf(windows);
    }

    public CalibrationRequest(String queueId, List<TelemetryWindow> windows) {
        this(queueId, windows, false, 42L);
    }
}
