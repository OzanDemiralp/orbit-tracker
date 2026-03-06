package com.ozandemiralp.orbit_tracker.service;

import com.ozandemiralp.orbit_tracker.dto.SatelliteDTO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TleParserService {

    public List<SatelliteDTO> parseTleResponse(String rawTle){
        List<SatelliteDTO> satellites = new ArrayList<>();

        String[] lines = rawTle.split("\\r?\\n");

        for (int i = 0; i < lines.length; i++) {
            if(i + 2 < lines.length){
                String name = lines[i].trim();
                String line1 = lines[i+1];
                String line2 = lines[i+2];

                satellites.add(new SatelliteDTO(name, line1, line2));
            }
        }
        return satellites;
    }

    public SatelliteDTO findSatelliteByName(List<SatelliteDTO> list, String name){
        return list.stream()
                .filter(s -> s.name().toUpperCase().contains(name.toUpperCase()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Satellite not found" + name));
    }
}

