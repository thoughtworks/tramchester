package com.tramchester.resources;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.domain.BoundingBoxWithCost;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.MyLocation;
import com.tramchester.domain.places.PostcodeLocation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.BoxWithCostDTO;
import com.tramchester.domain.presentation.DTO.PostcodeDTO;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.geo.GridPosition;
import com.tramchester.graph.search.FastestRoutesForBoxes;
import com.tramchester.graph.search.JourneyRequest;
import com.tramchester.mappers.JourneyToDTOMapper;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.postcodes.PostcodeRepository;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.opengis.referencing.operation.TransformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.LocalDate;
import java.util.Optional;
import java.util.stream.Stream;

import static java.lang.String.format;

@Api
@Path("/grid")
@Produces(MediaType.APPLICATION_JSON)
public class JourneysForGridResource {
    private static final Logger logger = LoggerFactory.getLogger(JourneysForGridResource.class);

    private final StationRepository repository;
    private final FastestRoutesForBoxes search;
    private final JourneyToDTOMapper dtoMapper;
    private final PostcodeRepository postcodeRepository;
    private final ObjectMapper objectMapper;

    @Inject
    public JourneysForGridResource(StationRepository repository, FastestRoutesForBoxes search, JourneyToDTOMapper dtoMapper,
                                   PostcodeRepository postcodeRepository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.search = search;
        this.dtoMapper = dtoMapper;
        this.postcodeRepository = postcodeRepository;
        this.objectMapper = objectMapper;
    }

    // TOOD Cache lifetime could potentially be quite long here, but makes testing harder.....

    @GET
    @Timed
    @ApiOperation(value = "Get cheapest travel costs for a grid of locations", response = BoundingBoxWithCost.class)
    //@CacheControl(maxAge = 30, maxAgeUnit = TimeUnit.SECONDS)
    public Response gridCosts(@QueryParam("gridSize") int gridSize,
                              @QueryParam("destination") String destinationIdText,
                              @QueryParam("departureTime") String departureTimeRaw,
                              @QueryParam("departureDate") String departureDateRaw,
                              @QueryParam("maxChanges") int maxChanges,
                              @QueryParam("maxDuration") int maxDuration,
                              @QueryParam("lat") @DefaultValue("0") String lat,
                              @QueryParam("lon") @DefaultValue("0") String lon) {
        logger.info(format("Query for quicktimes to %s for grid of size %s at %s %s maxchanges %s max duration %s",
                destinationIdText, gridSize, departureTimeRaw, departureDateRaw, maxChanges, maxDuration));

        GridPosition destination = getGridPosition(destinationIdText, lat, lon);

        Optional<TramTime> maybeDepartureTime = TramTime.parse(departureTimeRaw);
        if (maybeDepartureTime.isEmpty()) {
            logger.error("Could not parse departure time '" + departureTimeRaw + "'");
            return Response.serverError().build();
        }

        TramTime queryTime = maybeDepartureTime.get();
        LocalDate date = LocalDate.parse(departureDateRaw);

        // just find the first one -- todo this won't be lowest cost route....
        long maxNumberOfJourneys = 3;

        TramServiceDate tramServiceDate = new TramServiceDate(date);
        JourneyRequest journeyRequest = new JourneyRequest(tramServiceDate, queryTime,
                false, maxChanges, maxDuration, maxNumberOfJourneys);
        journeyRequest.setWarnIfNoResults(false);

        logger.info("Create search");
        Stream<BoxWithCostDTO> results = search.
                findForGrid(destination, gridSize, journeyRequest).
                map(box -> transformToDTO(box, tramServiceDate));
        logger.info("Creating stream");
        JsonStreamingOutput<BoxWithCostDTO> jsonStreamingOutput = new JsonStreamingOutput<>(results, objectMapper);

        logger.info("returning stream");
        Response.ResponseBuilder responseBuilder = Response.ok(jsonStreamingOutput);
        return responseBuilder.build();

    }

    private GridPosition getGridPosition(String idText, String lat, String lon) {
        GridPosition destination;
        if (MyLocation.isUserLocation(idText)) {
            LatLong latLong = decodeLatLong(lat, lon);
            destination = CoordinateTransforms.getGridPosition(latLong);
        } else if (PostcodeDTO.isPostcodeId(idText)) {
            IdFor<PostcodeLocation> postcodeId = PostcodeDTO.decodePostcodeId(idText);
            PostcodeLocation postcode = postcodeRepository.getPostcode(postcodeId);
            destination = postcode.getGridPosition();
        } else {
            IdFor<Station> stationId = StringIdFor.createId(idText);
            Station station = repository.getStationById(stationId);
            destination = station.getGridPosition();
        }
        return destination;
    }

    private BoxWithCostDTO transformToDTO(BoundingBoxWithCost box, TramServiceDate serviceDate) {
        try {
            return BoxWithCostDTO.createFrom(dtoMapper, serviceDate, box);
        } catch (TransformException exception) {
            throw new RuntimeException("Unable to convert coordinates ", exception);
        }
    }

    private LatLong decodeLatLong(String lat, String lon) {
        double latitude = Double.parseDouble(lat);
        double longitude = Double.parseDouble(lon);
        return new LatLong(latitude,longitude);
    }


}
