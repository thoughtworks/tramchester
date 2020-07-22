package com.tramchester.router;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.MyLocationFactory;
import com.tramchester.domain.places.PostcodeLocation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.domain.presentation.DTO.PostcodeDTO;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.graph.search.JourneyRequest;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.graph.search.RouteCalculatorArriveBy;
import com.tramchester.mappers.TramJourneyToDTOMapper;
import com.tramchester.repository.PostcodeRepository;
import com.tramchester.repository.TransportData;
import com.tramchester.resources.LocationJourneyPlanner;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Stream;

import static java.lang.String.format;

public class ProcessPlanRequest {
    private static final Logger logger = LoggerFactory.getLogger(ProcessPlanRequest.class);

    private final TramchesterConfig config;
    private final LocationJourneyPlanner locToLocPlanner;
    private final RouteCalculator routeCalculator;
    private final RouteCalculatorArriveBy routeCalculatorArriveBy;
    private final TransportData transportData;
    private final PostcodeRepository postcodeRepository;
    private final TramJourneyToDTOMapper tramJourneyToDTOMapper;

    public ProcessPlanRequest(TramchesterConfig config, LocationJourneyPlanner locToLocPlanner, RouteCalculator routeCalculator,
                              RouteCalculatorArriveBy routeCalculatorArriveBy, TransportData transportData,
                              PostcodeRepository postcodeRepository, TramJourneyToDTOMapper tramJourneyToDTOMapper) {
        this.config = config;
        this.locToLocPlanner = locToLocPlanner;

        this.routeCalculator = routeCalculator;
        this.routeCalculatorArriveBy = routeCalculatorArriveBy;
        this.transportData = transportData;
        this.postcodeRepository = postcodeRepository;
        this.tramJourneyToDTOMapper = tramJourneyToDTOMapper;
    }

    public Stream<JourneyDTO> directRequest(Transaction txn, String startId, String endId, JourneyRequest journeyRequest,
                                            String lat, String lon) {
        Stream<Journey> journeys;

        if (isFromUserLocation(startId)) {
            LatLong latLong = decodeLatLong(lat, lon);
            journeys = startsWithPosition(txn, latLong, endId, journeyRequest);
        } else if (isFromUserLocation(endId)) {
            LatLong latLong = decodeLatLong(lat, lon);
            journeys = endsWithPosition(txn, startId, latLong, journeyRequest);
        } else {
            journeys = createJourneyPlan(txn, startId, endId, journeyRequest);
        }

        return mapToDTOStream(journeyRequest.getDate(), journeys);
    }

    private Stream<JourneyDTO> mapToDTOStream(TramServiceDate queryDate, Stream<Journey> journeyStream) {
        return journeyStream.
                map(journey -> tramJourneyToDTOMapper.createJourneyDTO(journey, queryDate)).
                // TODO Check, ideally remove from here and push down into traverser code?
                limit(config.getMaxNumResults());
    }

    private Stream<Journey> createJourneyPlan(Transaction txn, String startId, String endId, JourneyRequest journeyRequest) {
        logger.info(format("Plan journey from %s to %s on %s", startId, endId, journeyRequest));

        boolean firstIsStation = !startId.startsWith(PostcodeDTO.PREFIX);
        boolean secondIsStation = !endId.startsWith(PostcodeDTO.PREFIX);

        if (firstIsStation && secondIsStation) {
            Station start = getStation(startId, "start");
            Station dest = getStation(endId, "end");
            return stationToStation(txn, start, dest, journeyRequest);
        }

        // Station -> Postcode
        if (firstIsStation) {
            PostcodeLocation dest = getPostcode(endId, "end");
            return endsWithPosition(txn, startId, dest.getLatLong(), journeyRequest);
        }

        // Postcode -> Station
        if (secondIsStation) {
            PostcodeLocation start = getPostcode(startId, "start");
            return startsWithPosition(txn, start.getLatLong(), endId, journeyRequest);
        }

        return postcodeToPostcode(startId, endId, journeyRequest, txn);
    }

    private Stream<Journey> postcodeToPostcode(String startId, String endId, JourneyRequest journeyRequest, Transaction txn) {
        Location start = getPostcode(startId, "start");
        Location dest = getPostcode(endId, "end");
        return locToLocPlanner.quickestRouteForLocation(txn, start.getLatLong(), dest.getLatLong(), journeyRequest);
    }

    private Stream<Journey> startsWithPosition(Transaction txn, LatLong latLong, String destId,
                                                         JourneyRequest journeyRequest) {
        logger.info(format("Plan journey from %s to %s on %s", latLong, destId, journeyRequest));

        Station dest = getStation(destId, "end");

        return locToLocPlanner.quickestRouteForLocation(txn, latLong, dest, journeyRequest);
    }

    private Stream<Journey> endsWithPosition(Transaction txn, String startId, LatLong latLong, JourneyRequest journeyRequest) {
        logger.info(format("Plan journey from %s to %s on %s", startId, latLong, journeyRequest));

        Station start = getStation(startId, "start");

        return locToLocPlanner.quickestRouteForLocation(txn, start, latLong, journeyRequest);
    }

    private Stream<Journey> stationToStation(Transaction txn, Station start, Station dest, JourneyRequest journeyRequest) {
        Stream<Journey> journeys;
        if (journeyRequest.getArriveBy()) {
            journeys = routeCalculatorArriveBy.calculateRoute(txn, start, dest, journeyRequest);
        } else {
            journeys = routeCalculator.calculateRoute(txn, start, dest, journeyRequest);
        }
        return journeys;
    }

    private PostcodeLocation getPostcode(String text, String diagnostic) {
        String locationId = text.replaceFirst(PostcodeDTO.PREFIX, "");
        if (!postcodeRepository.hasPostcode(locationId)) {
            String msg = "Unable to find " + diagnostic +" postcode from:  "+ locationId;
            logger.warn(msg);
            throw new RuntimeException(msg);
        }
        return postcodeRepository.getPostcode(locationId);
    }

    private Station getStation(String locationId, String diagnostic) {
        if (!transportData.hasStationId(locationId)) {
            String msg = "Unable to find " + diagnostic + " station from id: "+ locationId;
            logger.warn(msg);
            throw new RuntimeException(msg);
        }
        return transportData.getStationById(locationId);
    }

    private boolean isFromUserLocation(String startId) {
        return MyLocationFactory.MY_LOCATION_PLACEHOLDER_ID.equals(startId);
    }

    private LatLong decodeLatLong(String lat, String lon) {
        double latitude = Double.parseDouble(lat);
        double longitude = Double.parseDouble(lon);
        return new LatLong(latitude,longitude);
    }

}
