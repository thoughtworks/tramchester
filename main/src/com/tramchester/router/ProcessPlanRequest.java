package com.tramchester.router;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.id.CaseInsensitiveId;
import com.tramchester.domain.id.CompositeId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.*;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.domain.presentation.DTO.PostcodeDTO;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.mappers.JourneyToDTOMapper;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.StationRepositoryPublic;
import com.tramchester.repository.postcodes.PostcodeRepository;
import com.tramchester.resources.LocationJourneyPlanner;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.stream.Stream;

import static java.lang.String.format;

@LazySingleton
public class ProcessPlanRequest {
    private static final Logger logger = LoggerFactory.getLogger(ProcessPlanRequest.class);

    private final LocationJourneyPlanner locToLocPlanner;

    private final StationRepositoryPublic stationRepository;
    private final PostcodeRepository postcodeRepository;
    private final JourneyToDTOMapper journeyToDTOMapper;

    @Inject
    public ProcessPlanRequest(TramchesterConfig config, LocationJourneyPlanner locToLocPlanner, StationRepository stationRepository,
                              PostcodeRepository postcodeRepository, JourneyToDTOMapper journeyToDTOMapper) {
        this.locToLocPlanner = locToLocPlanner;


        this.stationRepository = stationRepository;
        this.postcodeRepository = postcodeRepository;
        this.journeyToDTOMapper = journeyToDTOMapper;
    }

    public Stream<JourneyDTO> directRequest(Transaction txn, Location<?> start, Location<?> dest, JourneyRequest journeyRequest) {
        Stream<Journey> journeys = locationToLocation(txn, start, dest, journeyRequest);
        return mapToDTOStream(journeyRequest.getDate(), journeys);
    }

    @Deprecated
    public Stream<JourneyDTO> directRequest(Transaction txn, String startId, String endId, JourneyRequest journeyRequest,
                                            String lat, String lon) {
        Stream<Journey> journeys;

        if (MyLocation.isUserLocation(startId)) {
            MyLocation start = new MyLocation(decodeLatLong(lat, lon));
            Station dest = getStation(endId, "end");
            journeys = locationToLocation(txn, start, dest, journeyRequest);
        } else if (MyLocation.isUserLocation(endId)) {
            Station start = getStation(startId, "start");
            MyLocation dest = new MyLocation(decodeLatLong(lat, lon));
            journeys = locationToLocation(txn, start, dest, journeyRequest);
        } else {
            journeys = createJourneyPlan(txn, startId, endId, journeyRequest);
        }

        return mapToDTOStream(journeyRequest.getDate(), journeys);
    }

    private Stream<JourneyDTO> mapToDTOStream(TramServiceDate queryDate, Stream<Journey> journeyStream) {
        return journeyStream.
                filter(journey -> !journey.getStages().isEmpty()).
                map(journey -> journeyToDTOMapper.createJourneyDTO(journey, queryDate));
    }

    @Deprecated
    private Stream<Journey> createJourneyPlan(Transaction txn, String startId, String endId, JourneyRequest journeyRequest) {
        logger.info(format("Plan journey from %s to %s on %s", startId, endId, journeyRequest));

        boolean firstIsStation = !PostcodeDTO.isPostcodeId(startId);
        boolean secondIsStation = !PostcodeDTO.isPostcodeId(endId);

        if (firstIsStation && secondIsStation) {
            Station start = getStation(startId, "start");
            Station dest = getStation(endId, "end");
            return locationToLocation(txn, start, dest, journeyRequest);
        }

        // Station -> Postcode
        if (firstIsStation) {
            Station start = getStation(startId, "start");
            PostcodeLocation dest = getPostcode(endId, "end");
            return locationToLocation(txn, start, dest, journeyRequest);
        }

        // Postcode -> Station
        if (secondIsStation) {
            PostcodeLocation start = getPostcode(startId, "start");
            Station dest = getStation(endId, "end");
            return locationToLocation(txn, start, dest, journeyRequest);
        }

        PostcodeLocation start = getPostcode(startId, "start");
        PostcodeLocation dest = getPostcode(endId, "end");
        return locationToLocation(txn, start, dest, journeyRequest);
    }

    private Stream<Journey> locationToLocation(Transaction txn, Location<?> start, Location<?> dest,
                                               JourneyRequest journeyRequest) {
        logger.info(format("Plan journey from %s to %s on %s", start, dest, journeyRequest));

        return locToLocPlanner.quickestRouteForLocation(txn, start, dest, journeyRequest);
    }

    @Deprecated
    private PostcodeLocation getPostcode(String text, String diagnostic) {

        CaseInsensitiveId<PostcodeLocation> postcodeId = PostcodeDTO.decodePostcodeId(text);

        if (!postcodeRepository.hasPostcode(postcodeId)) {
            String msg = "Unable to find " + diagnostic +" postcode from:  "+ postcodeId;
            logger.warn(msg);
            throw new RuntimeException(msg);
        }
        return postcodeRepository.getPostcode(postcodeId);
    }

    @Deprecated
    private Station getStation(String text, String diagnostic) {

        IdFor<Station> stationId = StringIdFor.createId(text);
        if (stationRepository.hasStationId(stationId)) {
            return stationRepository.getStationById(stationId);
        }
        // failed, now try composite ID
        CompositeId<Station> compositeId = CompositeId.parse(text);
        if (stationRepository.hasStationId(compositeId)) {
            return stationRepository.getStationById(compositeId);
        }

        String msg = "Unable to find " + diagnostic + " station from id: " + text;
        logger.warn(msg);
        throw new RuntimeException(msg);
    }

    private LatLong decodeLatLong(String lat, String lon) {
        double latitude = Double.parseDouble(lat);
        double longitude = Double.parseDouble(lon);
        return new LatLong(latitude,longitude);
    }

}
