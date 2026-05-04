package com.ozandemiralp.orbit_tracker.service;

import com.ozandemiralp.orbit_tracker.dto.SatelliteCurrentPositionRequestDTO;
import com.ozandemiralp.orbit_tracker.dto.SatelliteCurrentPositionResponseDTO;
import lombok.AllArgsConstructor;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.frames.Frame;
import org.orekit.frames.StaticTransform;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
public class OrbitService {

    private final TleCacheService tleCacheService;

    private final Frame itrf;
    private final Frame teme;
    private final BodyShape earth;
    private final int DEFAULT_STEPS = 100;
    private final double MAX_DURATION_SECONDS = 86400;

    public Mono<SatelliteCurrentPositionResponseDTO> getCurrentSatellitePosition(SatelliteCurrentPositionRequestDTO request) {
        return tleCacheService.getSatelliteMap(request.satelliteGroup())
                .flatMap(satelliteMap ->
                        Mono.fromCallable(() -> {
                            TLE targetTLE = findInMap(satelliteMap, request.satelliteName());
                            TLEPropagator propagator = TLEPropagator.selectExtrapolator(targetTLE);

                            AbsoluteDate currentDate = new AbsoluteDate(new Date(), TimeScalesFactory.getUTC());
                            return calculatePositionAtDate(propagator, currentDate);
                        }).subscribeOn(Schedulers.parallel())
                );
    }

    public Mono<List<SatelliteCurrentPositionResponseDTO>> getTrajectory(SatelliteCurrentPositionRequestDTO request) {
        return tleCacheService.getSatelliteMap(request.satelliteGroup())
                .flatMap(satelliteMap ->
                        Mono.fromCallable(() -> {
                    TLE targetTLE = findInMap(satelliteMap, request.satelliteName());
                    TLEPropagator propagator = TLEPropagator.selectExtrapolator(targetTLE);

                            double stepSize = MAX_DURATION_SECONDS / DEFAULT_STEPS;

                            List<SatelliteCurrentPositionResponseDTO> trajectory = new ArrayList<>(DEFAULT_STEPS + 1);
                            AbsoluteDate startDate = new AbsoluteDate(new Date(), TimeScalesFactory.getUTC());

                            for (int i = 0; i <= DEFAULT_STEPS; i++) {
                                AbsoluteDate targetDate = startDate.shiftedBy(i * stepSize);
                                trajectory.add(calculatePositionAtDate(propagator, targetDate));
                            }

                            return trajectory;
                        }).subscribeOn(Schedulers.parallel())
                );
    }

    private TLE findInMap(Map<String, TLE> map, String name) {
        TLE foundTLE = map.get(name.toUpperCase());
        if (foundTLE == null) throw new RuntimeException("Satellite not found: " + name);
        return foundTLE;
    }

    private SatelliteCurrentPositionResponseDTO calculatePositionAtDate(TLEPropagator propagator, AbsoluteDate date) {
        SpacecraftState currentState = propagator.propagate(date);

        StaticTransform transform = teme.getStaticTransformTo(itrf, date);
        Vector3D positionAtItrf = transform.transformPosition(currentState.getPVCoordinates().getPosition());
        GeodeticPoint point = earth.transform(positionAtItrf, itrf, date);

        return new SatelliteCurrentPositionResponseDTO(
                currentState.getDate().toDate(TimeScalesFactory.getUTC()).toInstant(),
                FastMath.toDegrees(point.getLatitude()),
                FastMath.toDegrees(point.getLongitude()),
                point.getAltitude(),
                currentState.getPVCoordinates().getVelocity().getNorm() / 1000.0
        );
    }
}