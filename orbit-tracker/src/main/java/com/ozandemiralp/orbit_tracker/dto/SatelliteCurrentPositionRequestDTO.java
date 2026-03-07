package com.ozandemiralp.orbit_tracker.dto;

public record SatelliteCurrentPositionRequestDTO(
        String satelliteGroup,
        String satelliteName
) {
}
