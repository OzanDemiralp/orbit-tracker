package com.ozandemiralp.orbit_tracker.service;

import com.ozandemiralp.orbit_tracker.client.CelestrakClient;
import com.ozandemiralp.orbit_tracker.dto.SatelliteCurrentPositionRequestDTO;
import com.ozandemiralp.orbit_tracker.dto.SatelliteCurrentPositionResponseDTO;
import com.ozandemiralp.orbit_tracker.dto.SatelliteDTO;
import lombok.AllArgsConstructor;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.StaticTransform;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@AllArgsConstructor
public class OrbitService {

    private final CelestrakClient celestrakClient;
    private final TleParserService tleParserService;

    public Mono<SatelliteCurrentPositionResponseDTO> getCurrentSatellitePosition(SatelliteCurrentPositionRequestDTO request){
        return celestrakClient.getTleDataByGroup(request.satelliteGroup())
                .map(rawTle -> {
                    OrbitContext orbitContext = prepareOrbitContext(rawTle, request.satelliteName());
                    AbsoluteDate currentDate = new AbsoluteDate(new Date(), TimeScalesFactory.getUTC());

                    return calculatePositionAtDate(orbitContext, currentDate);
                });
    }

    public Mono<List<SatelliteCurrentPositionResponseDTO>> getTrajectory(SatelliteCurrentPositionRequestDTO request){
        return celestrakClient.getTleDataByGroup(request.satelliteGroup())
                .map(rawTle -> {
                    OrbitContext orbitContext = prepareOrbitContext(rawTle, request.satelliteName());

                    // Period and step size
                    double period = orbitContext.propagator().getInitialState().getOrbit().getKeplerianPeriod();
                    double duration = Math.min(period, 86400.0);
                    int steps = 100;
                    double stepSize = duration / steps;

                    // Positions
                    List<SatelliteCurrentPositionResponseDTO> trajectory = new ArrayList<>();
                    AbsoluteDate startDate = new AbsoluteDate(new Date(), TimeScalesFactory.getUTC());

                    for (int i = 0; i <= steps; i++) {
                        AbsoluteDate targetDate = startDate.shiftedBy(i * stepSize);
                        trajectory.add(calculatePositionAtDate(orbitContext, targetDate));
                    }
                    return trajectory;
                });
    }

    private SatelliteCurrentPositionResponseDTO calculatePositionAtDate(OrbitContext orbitContext, AbsoluteDate date
                                                                        ) {
        SpacecraftState currentState = orbitContext.propagator().propagate(date);
        StaticTransform transform = orbitContext.teme.getStaticTransformTo(orbitContext.itrf(), date);
        Vector3D positionAtItrf = transform.transformPosition(currentState.getPVCoordinates().getPosition());
        GeodeticPoint point = orbitContext.earth().transform(positionAtItrf, orbitContext.itrf(), date);

        return new SatelliteCurrentPositionResponseDTO(
                currentState.getDate().toDate(TimeScalesFactory.getUTC()).toInstant(),
                FastMath.toDegrees(point.getLatitude()),
                FastMath.toDegrees(point.getLongitude()),
                point.getAltitude(),
                currentState.getPVCoordinates().getVelocity().getNorm() / 1000.0
        );
    }

    private record OrbitContext(TLEPropagator propagator, BodyShape earth, Frame itrf, Frame teme) {}

    private OrbitContext prepareOrbitContext(String rawTle, String satelliteName) {
        List<SatelliteDTO> allSatellites = tleParserService.parseTleResponse(rawTle);
        SatelliteDTO targetSatellite = tleParserService.findSatelliteByName(allSatellites, satelliteName);
        TLE tle = new TLE(targetSatellite.tleLine1(), targetSatellite.tleLine2());
        TLEPropagator propagator = TLEPropagator.selectExtrapolator(tle);
        Frame itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        Frame teme = FramesFactory.getTEME();
        BodyShape earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                Constants.WGS84_EARTH_FLATTENING, itrf);

        return new OrbitContext(propagator, earth, itrf, teme);
    }
}

