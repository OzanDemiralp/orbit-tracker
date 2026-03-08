package com.ozandemiralp.orbit_tracker.dto;

import java.time.Instant;

public record SatelliteCurrentPositionResponseDTO(
        Instant timestamp,
        double latitude,
        double longitude,
        double altitude,
        double velocity
) {
}
