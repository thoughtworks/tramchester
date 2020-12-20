package com.tramchester.resources;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.domain.BoundingBoxWithCost;
import com.tramchester.domain.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.BoxWithCostDTO;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.search.FastestRoutesForBoxes;
import com.tramchester.graph.search.JourneyRequest;
import com.tramchester.mappers.JourneyToDTOMapper;
import com.tramchester.repository.StationRepository;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.opengis.referencing.operation.TransformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
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
    private final ObjectMapper objectMapper;

    @Inject
    public JourneysForGridResource(StationRepository repository, FastestRoutesForBoxes search, JourneyToDTOMapper dtoMapper,
                                   ObjectMapper objectMapper) {
        this.repository = repository;
        this.search = search;
        this.dtoMapper = dtoMapper;
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
                              @QueryParam("maxDuration") int maxDuration) {
        logger.info(format("Query for quicktimes to %s for grid of size %s at %s %s maxchanges %s max duration %s",
                destinationIdText, gridSize, departureTimeRaw, departureDateRaw, maxChanges, maxDuration));

        IdFor<Station> destinationId = IdFor.createId(destinationIdText);
        Station destination = repository.getStationById(destinationId);

        Optional<TramTime> maybeDepartureTime = TramTime.parse(departureTimeRaw);
        if (maybeDepartureTime.isEmpty()) {
            logger.error("Could not parse departure time '" + departureTimeRaw + "'");
            return Response.serverError().build();
        }

        TramTime queryTime = maybeDepartureTime.get();
        LocalDate date = LocalDate.parse(departureDateRaw);

        TramServiceDate tramServiceDate = new TramServiceDate(date);
        JourneyRequest journeyRequest = new JourneyRequest(tramServiceDate, queryTime,
                false, maxChanges, maxDuration);
        journeyRequest.setWarnIfNoResults(false);
        long numberToFind = maxChanges;

        logger.info("Create search");
        Stream<BoxWithCostDTO> results = search.
                findForGrid(destination, gridSize, journeyRequest, numberToFind).
                map(box -> transformToDTO(box, tramServiceDate));
        logger.info("Creating stream");
        JsonStreamingOutput<BoxWithCostDTO> jsonStreamingOutput = new JsonStreamingOutput<>(results, objectMapper);

        logger.info("returning stream");
        Response.ResponseBuilder responseBuilder = Response.ok(jsonStreamingOutput);
        return responseBuilder.build();

    }

    private BoxWithCostDTO transformToDTO(BoundingBoxWithCost box, TramServiceDate serviceDate) {
        try {
            return BoxWithCostDTO.createFrom(dtoMapper, serviceDate, box);
        } catch (TransformException exception) {
            throw new RuntimeException("Unable to convert coordinates ", exception);
        }
    }

}
