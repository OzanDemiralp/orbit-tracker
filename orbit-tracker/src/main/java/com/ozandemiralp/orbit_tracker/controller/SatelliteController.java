package com.ozandemiralp.orbit_tracker.controller;

import com.ozandemiralp.orbit_tracker.client.CelestrakClient;
import com.ozandemiralp.orbit_tracker.dto.SatelliteDTO;
import com.ozandemiralp.orbit_tracker.dto.SatelliteTleRequestDTO;
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

    @PostMapping("/satelliteTle")
    public Mono<SatelliteDTO> getSatelliteTle(@RequestBody SatelliteTleRequestDTO requestDTO){
        return celestrakClient.getTleDataByGroup(requestDTO.group())
                .map(rawTle -> {
                    List<SatelliteDTO> allSatellites = tleParserService.parseTleResponse(rawTle);
                    return tleParserService.findSatelliteByName(allSatellites, requestDTO.satelliteName());
                });
        }

}
