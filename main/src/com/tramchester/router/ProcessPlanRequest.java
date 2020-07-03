package com.tramchester.router;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.MyLocationFactory;
import com.tramchester.domain.places.PostcodeLocation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.domain.presentation.DTO.JourneyPlanRepresentation;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.graph.search.JourneyRequest;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.graph.search.RouteCalculatorArriveBy;
import com.tramchester.mappers.TramJourneyToDTOMapper;
import com.tramchester.repository.PostcodeRepository;
import com.tramchester.repository.TransportData;
import com.tramchester.resources.LocationJourneyPlanner;
import org.jetbrains.annotations.NotNull;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.stream.Collectors;
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

    public JourneyPlanRepresentation directRequest(Transaction txn, String startId, String endId, JourneyRequest journeyRequest,
                                                   String lat, String lon) {
        JourneyPlanRepresentation planRepresentation;
        if (isFromUserLocation(startId)) {
            LatLong latLong = decodeLatLong(lat, lon);
            planRepresentation = startsWithPosition(txn, latLong, endId, journeyRequest);
        } else if (isFromUserLocation(endId)) {
            LatLong latLong = decodeLatLong(lat, lon);
            planRepresentation = endsWithPosition(txn, startId, latLong, journeyRequest);
        } else {
            planRepresentation = createJourneyPlan(txn, startId, endId, journeyRequest);
        }
        return planRepresentation;
    }

    private JourneyPlanRepresentation createJourneyPlan(Transaction txn, String startId, String endId, JourneyRequest journeyRequest) {
        logger.info(format("Plan journey from %s to %s on %s", startId, endId, journeyRequest));

        boolean firstIsStation = startsWithDigit(startId);
        boolean secondIsStation = startsWithDigit(endId);

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

    @NotNull
    private JourneyPlanRepresentation postcodeToPostcode(String startId, String endId, JourneyRequest journeyRequest, Transaction txn) {
        Location start = getPostcode(startId, "start");
        Location dest = getPostcode(endId, "end");
        Stream<Journey> journeys =  locToLocPlanner.quickestRouteForLocation(txn, start.getLatLong(), dest.getLatLong(), journeyRequest);
        JourneyPlanRepresentation plan = createPlan(journeyRequest.getDate(), journeys);
        journeys.close();
        return plan;
    }

    private JourneyPlanRepresentation startsWithPosition(Transaction txn, LatLong latLong, String destId,
                                                         JourneyRequest journeyRequest) {
        logger.info(format("Plan journey from %s to %s on %s", latLong, destId, journeyRequest));

        Station dest = getStation(destId, "end");

        Stream<Journey> journeys = locToLocPlanner.quickestRouteForLocation(txn, latLong, dest, journeyRequest);
        JourneyPlanRepresentation plan = createPlan(journeyRequest.getDate(), journeys);
        journeys.close();
        return plan;
    }

    private JourneyPlanRepresentation endsWithPosition(Transaction txn, String startId, LatLong latLong, JourneyRequest journeyRequest) {
        logger.info(format("Plan journey from %s to %s on %s", startId, latLong, journeyRequest));

        Station start = getStation(startId, "start");

        Stream<Journey> journeys = locToLocPlanner.quickestRouteForLocation(txn, start, latLong, journeyRequest);
        JourneyPlanRepresentation plan = createPlan(journeyRequest.getDate(), journeys);
        journeys.close();
        return plan;
    }

    private JourneyPlanRepresentation stationToStation(Transaction txn, Station start, Station dest, JourneyRequest journeyRequest) {
        Stream<Journey> journeys;
        if (journeyRequest.getArriveBy()) {
            journeys = routeCalculatorArriveBy.calculateRoute(txn, start, dest, journeyRequest);
        } else {
            journeys = routeCalculator.calculateRoute(txn, start, dest, journeyRequest);
        }
        // ASSUME: Limit here rely's on search giving lowest cost routes first
        // TODO Check, ideally remove from here and push down into traverser code?
        JourneyPlanRepresentation journeyPlanRepresentation = createPlan(journeyRequest.getDate(),
                journeys.limit(config.getMaxNumResults()));
        journeys.close();
        return journeyPlanRepresentation;
    }

    private PostcodeLocation getPostcode(String locationId, String diagnostic) {
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
        return transportData.getStation(locationId);
    }

    private boolean startsWithDigit(String startId) {
        return Character.isDigit(startId.charAt(0));
    }

    private JourneyPlanRepresentation createPlan(TramServiceDate queryDate, Stream<Journey> journeyStream) {

        Set<JourneyDTO> journeyDTOs = journeyStream.
                map(journey -> tramJourneyToDTOMapper.createJourneyDTO(journey, queryDate)).
                limit(config.getMaxNumResults()).
                collect(Collectors.toSet());

        return new JourneyPlanRepresentation(journeyDTOs);
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
