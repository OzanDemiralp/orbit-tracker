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
import org.orekit.frames.Frames;
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

import java.time.Instant;
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
                    // Find tle
                    List<SatelliteDTO> allSatellites =  tleParserService.parseTleResponse(rawTle);
                    SatelliteDTO targetSatellite = tleParserService.findSatelliteByName(allSatellites,
                            request.satelliteName());

                    // Propagator, Frame and BodyShape
                    TLE tle = new TLE(targetSatellite.tleLine1(), targetSatellite.tleLine2());
                    TLEPropagator propagator = TLEPropagator.selectExtrapolator(tle);
                    Frame itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
                    BodyShape earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                            Constants.WGS84_EARTH_FLATTENING, itrf);

                    // Current position in Teme
                    AbsoluteDate now = new AbsoluteDate(new Date(), TimeScalesFactory.getUTC());
                    SpacecraftState currentState = propagator.propagate(now);
                    Vector3D positionInTeme = currentState.getPVCoordinates().getPosition();

                    // Transform position to itrf
                    StaticTransform transform = FramesFactory.getTEME().getStaticTransformTo(itrf, now);
                    Vector3D positionInItrf = transform.transformPosition(positionInTeme);
                    GeodeticPoint point = earth.transform(positionInItrf, itrf, now);

                    // Results
                    Instant timestamp = currentState.getDate().toDate(TimeScalesFactory.getUTC()).toInstant();
                    double latitude = FastMath.toDegrees(point.getLatitude());
                    double longitude = FastMath.toDegrees(point.getLongitude());
                    double altitude = point.getAltitude() / 1000.0;
                    double velocity = currentState.getPVCoordinates().getVelocity().getNorm() / 1000;

                    return new SatelliteCurrentPositionResponseDTO(timestamp, latitude, longitude, altitude, velocity);
                });
    }

    public Mono<List<SatelliteCurrentPositionResponseDTO>> getTrajectory(SatelliteCurrentPositionRequestDTO request){
        return celestrakClient.getTleDataByGroup(request.satelliteGroup())
                .map(rawTle -> {
                    // Find tle
                    List<SatelliteDTO> allSatellites =  tleParserService.parseTleResponse(rawTle);
                    SatelliteDTO targetSatellite = tleParserService.findSatelliteByName(allSatellites,
                            request.satelliteName());

                    // Propagator, Frame and BodyShape
                    TLE tle = new TLE(targetSatellite.tleLine1(), targetSatellite.tleLine2());
                    TLEPropagator propagator = TLEPropagator.selectExtrapolator(tle);
                    Frame itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
                    BodyShape earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                            Constants.WGS84_EARTH_FLATTENING, itrf);

                    // Period and step size
                    double period = propagator.getInitialState().getOrbit().getKeplerianPeriod();
                    double duration = Math.min(period, 86400.0);
                    int steps = 100;
                    double stepSize = duration / steps;

                    // Positions
                    List<SatelliteCurrentPositionResponseDTO> trajectory = new ArrayList<>();
                    AbsoluteDate startDate = new AbsoluteDate(new Date(), TimeScalesFactory.getUTC());

                    for (int i = 0; i <= steps; i++) {
                        AbsoluteDate targetDate = startDate.shiftedBy(i * stepSize);
                        SpacecraftState currentState = propagator.propagate(targetDate);

                        // Transform position to itrf
                        StaticTransform transform = FramesFactory.getTEME().getStaticTransformTo(itrf, targetDate);
                        Vector3D posItrf = transform.transformPosition(currentState.getPVCoordinates().getPosition());
                        GeodeticPoint point = earth.transform(posItrf, itrf, targetDate);

                        // Results
                        Instant timestamp = currentState.getDate().toDate(TimeScalesFactory.getUTC()).toInstant();
                        double latitude = FastMath.toDegrees(point.getLatitude());
                        double longitude = FastMath.toDegrees(point.getLongitude());
                        double altitude = point.getAltitude() / 1000.0;
                        double velocity = currentState.getPVCoordinates().getVelocity().getNorm() / 1000;

                        trajectory.add(new SatelliteCurrentPositionResponseDTO(
                                timestamp,
                                latitude,
                                longitude,
                                altitude,
                                velocity
                        ));
                    }

                    return trajectory;
                });
    }
}

