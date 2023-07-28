package com.tramchester.resources;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import com.google.inject.Inject;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.BoundingBoxWithCost;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.PostcodeLocationId;
import com.tramchester.domain.places.MyLocation;
import com.tramchester.domain.places.PostcodeLocation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.BoxWithCostDTO;
import com.tramchester.domain.presentation.DTO.PostcodeDTO;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.geo.GridPosition;
import com.tramchester.graph.search.FastestRoutesForBoxes;
import com.tramchester.mappers.JourneyToDTOMapper;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.postcodes.PostcodeRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.core.Response;
import java.time.Duration;
import java.util.EnumSet;
import java.util.stream.Stream;

import static java.lang.String.format;

@Path("/grid")
@Produces(MediaType.APPLICATION_JSON)
public class JourneysForGridResource implements APIResource, GraphDatabaseDependencyMarker {
    private static final Logger logger = LoggerFactory.getLogger(JourneysForGridResource.class);

    private final StationRepository repository;
    private final FastestRoutesForBoxes search;
    private final JourneyToDTOMapper dtoMapper;
    private final PostcodeRepository postcodeRepository;
    private final TramchesterConfig config;
    private final ObjectMapper objectMapper;

    @Inject
    public JourneysForGridResource(StationRepository repository, FastestRoutesForBoxes search, JourneyToDTOMapper dtoMapper,
                                   PostcodeRepository postcodeRepository, TramchesterConfig config) {
        this.config = config;
        logger.info("created");
        this.repository = repository;
        this.search = search;
        this.dtoMapper = dtoMapper;
        this.postcodeRepository = postcodeRepository;
        this.objectMapper = JsonMapper.builder().addModule(new AfterburnerModule()).build();
    }

    // TODO Cache lifetime could potentially be quite long here, but makes testing harder.....
    // TODO Switch to a POST and Query DTO instead

    @GET
    @Timed
    @Operation(description = "Get cheapest travel costs for a grid of locations")
    @ApiResponse(content = @Content(schema = @Schema(implementation = BoundingBoxWithCost.class)))
    //@CacheControl(maxAge = 30, maxAgeUnit = TimeUnit.SECONDS)
    public Response gridCosts(@QueryParam("gridSize") int gridSize,
                              @QueryParam("destination") String destinationIdText,
                              @QueryParam("departureTime") String departureTimeRaw,
                              @QueryParam("departureDate") String departureDateRaw,
                              @QueryParam("maxChanges") int maxChanges,
                              @QueryParam("maxDuration") int maxDurationMinutes,
                              @QueryParam("lat") @DefaultValue("0") String lat,
                              @QueryParam("lon") @DefaultValue("0") String lon) {
        logger.info(format("Query for quicktimes to %s for grid of size %s at %s %s maxchanges %s max duration %s",
                destinationIdText, gridSize, departureTimeRaw, departureDateRaw, maxChanges, maxDurationMinutes));

        GridPosition destination = getGridPosition(destinationIdText, lat, lon);

        TramTime departureTime = TramTime.parse(departureTimeRaw);
        if (!departureTime.isValid()) {
            logger.error("Could not parse departure time '" + departureTimeRaw + "'");
            return Response.serverError().build();
        }

        TramDate date = TramDate.parse(departureDateRaw);

        // just find the first one -- todo this won't be lowest cost route....
        long maxNumberOfJourneys = 3;

        Duration maxDuration = Duration.ofMinutes(maxDurationMinutes);

        //TramDate tramServiceDate = TramDate.of(date);
        EnumSet<TransportMode> allModes = config.getTransportModes();
        JourneyRequest journeyRequest = new JourneyRequest(date, departureTime,
                false, maxChanges, maxDuration, maxNumberOfJourneys, allModes);
        journeyRequest.setWarnIfNoResults(false);

        logger.info("Create search");
        Stream<BoxWithCostDTO> results = search.
                findForGrid(destination, gridSize, journeyRequest).
                map(box -> transformToDTO(box, date));
        
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
            PostcodeLocationId postcodeId = PostcodeDTO.decodePostcodeId(idText);
            PostcodeLocation postcode = postcodeRepository.getPostcode(postcodeId);
            destination = postcode.getGridPosition();
        } else {
            IdFor<Station> stationId = Station.createId(idText);
            Station station = repository.getStationById(stationId);
            destination = station.getGridPosition();
        }
        return destination;
    }

    private BoxWithCostDTO transformToDTO(BoundingBoxWithCost box, TramDate serviceDate) {
        return BoxWithCostDTO.createFrom(dtoMapper, serviceDate, box);
    }

    private LatLong decodeLatLong(String lat, String lon) {
        double latitude = Double.parseDouble(lat);
        double longitude = Double.parseDouble(lon);
        return new LatLong(latitude,longitude);
    }


}
