package com.ozandemiralp.orbit_tracker.controller;

import com.ozandemiralp.orbit_tracker.client.CelestrakClient;
import com.ozandemiralp.orbit_tracker.dto.SatelliteCurrentPositionRequestDTO;
import com.ozandemiralp.orbit_tracker.dto.SatelliteCurrentPositionResponseDTO;
import com.ozandemiralp.orbit_tracker.dto.SatelliteDTO;
import com.ozandemiralp.orbit_tracker.dto.SatelliteTleRequestDTO;
import com.ozandemiralp.orbit_tracker.service.OrbitService;
import com.ozandemiralp.orbit_tracker.service.TleParserService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/v1/satellites")
@AllArgsConstructor
public class SatelliteController {

    private final TleParserService tleParserService;
    private final CelestrakClient celestrakClient;
    private final OrbitService orbitService;

    @PostMapping("/satelliteTle")
    public Mono<SatelliteDTO> getSatelliteTle(@RequestBody SatelliteTleRequestDTO requestDTO){
        return celestrakClient.getTleDataByGroup(requestDTO.satelliteGroup())
                .map(rawTle -> {
                    List<SatelliteDTO> allSatellites = tleParserService.parseTleResponse(rawTle);
                    return tleParserService.findSatelliteByName(allSatellites, requestDTO.satelliteName());
                });
        }

        @PostMapping("/satellitePosition")
    public Mono<SatelliteCurrentPositionResponseDTO> getSatellitePosition(@RequestBody SatelliteCurrentPositionRequestDTO requestDTO){
        return orbitService.getCurrentSatellitePosition(requestDTO);
        }

}
