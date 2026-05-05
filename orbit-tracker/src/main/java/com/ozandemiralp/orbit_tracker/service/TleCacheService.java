package com.ozandemiralp.orbit_tracker.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.ozandemiralp.orbit_tracker.client.CelestrakClient;
import com.ozandemiralp.orbit_tracker.dto.SatelliteDTO;
import com.ozandemiralp.orbit_tracker.exception.TleServiceException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.orekit.propagation.analytical.tle.TLE;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Service
@AllArgsConstructor
public class TleCacheService {
    private final CelestrakClient celestrakClient;
    private final TleParserService tleParserService;

    private final Cache<String, Mono<Map<String, TLE>>> internalCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(90))
            .maximumSize(500)
            .build();
    
    private final Cache<String, Map<String, TLE>> staleCacheWithTTL = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(24 * 60))
            .maximumSize(500)
            .build();

    public Mono<Map<String, TLE>> getSatelliteMap(String group) {
        return internalCache.get(group, g ->
                celestrakClient.getTleDataByGroup(g)
                        .subscribeOn(Schedulers.boundedElastic())
                        .map(tleData -> {
                            Map<String, TLE> parsed = tleParserService.parseTleToMap(tleData);
                            staleCacheWithTTL.put(group, parsed);
                            return parsed;
                        })
                        .onErrorResume(ex -> {
                            log.error("Failed to fetch TLE data from Celestrak for group: {}", group, ex);
                            Map<String, TLE> staleData = staleCacheWithTTL.getIfPresent(group);
                            if (staleData != null && !staleData.isEmpty()) {
                                log.warn("Using stale cached TLE data for group: {} (Celestrak unavailable)", group);
                                return Mono.just(staleData);
                            }
                            
                            // if no cache is available return an error
                            String errorMsg = "Unable to fetch TLE data from Celestrak for group: " + group + 
                                            " and no cached data is available";
                            log.error(errorMsg);
                            return Mono.error(new TleServiceException(errorMsg, ex));
                        })
                        .cache(Duration.ofMinutes(90))
        );
    }
}
