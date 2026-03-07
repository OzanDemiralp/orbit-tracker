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

import java.util.Date;
import java.util.List;

@Service
@AllArgsConstructor
public class OrbitService {

    private final CelestrakClient celestrakClient;
    private final TleParserService tleParserService;

    public Mono<SatelliteCurrentPositionResponseDTO> getCurrentSatellitePosition(SatelliteCurrentPositionRequestDTO request){

        return celestrakClient.getTleDataByGroup(request.satelliteGroup())
                .map(rawTLe -> {
                    // Find tle
                    List<SatelliteDTO> allSatellites =  tleParserService.parseTleResponse(rawTLe);
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
                    double latitude = FastMath.toDegrees(point.getLatitude());
                    double longitude = FastMath.toDegrees(point.getLongitude());
                    double altitude = point.getAltitude() / 1000.0;

                    return new SatelliteCurrentPositionResponseDTO(latitude, longitude, altitude);
                });
    }
}

