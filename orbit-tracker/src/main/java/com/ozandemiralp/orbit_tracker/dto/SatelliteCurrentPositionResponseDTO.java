package com.ozandemiralp.orbit_tracker.dto;

public record SatelliteCurrentPositionResponseDTO(
        double latitude,
        double longitude,
        double altitude
) {
}
