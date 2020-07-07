package com.tramchester.resources;

import com.codahale.metrics.annotation.Timed;
import com.tramchester.domain.BoundingBoxWithCost;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.BoxWithCostDTO;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.search.FastestRoutesForBoxes;
import com.tramchester.graph.search.JourneyRequest;
import com.tramchester.repository.StationRepository;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.neo4j.graphdb.Transaction;
import org.opengis.referencing.operation.TransformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class JourneysForGridResource implements APIResource {
    private static final Logger logger = LoggerFactory.getLogger(JourneysForGridResource.class);

    private final StationRepository repository;
    private final FastestRoutesForBoxes search;
    private final GraphDatabase graphDatabaseService;
    private final CoordinateTransforms coordinateTransforms;

    public JourneysForGridResource(StationRepository repository, FastestRoutesForBoxes search, GraphDatabase graphDatabaseService, CoordinateTransforms coordinateTransforms) {
        this.repository = repository;
        this.search = search;
        this.graphDatabaseService = graphDatabaseService;
        this.coordinateTransforms = coordinateTransforms;
    }

    // TOOD Cache lifetime could potentially be quite long here, but makes testing harder.....

    @GET
    @Timed
    @ApiOperation(value = "Get cheapest travel costs for a grid of locations", response = BoundingBoxWithCost.class)
    //@CacheControl(maxAge = 30, maxAgeUnit = TimeUnit.SECONDS)
    public Response gridCosts(@QueryParam("gridSize") int gridSize,
                              @QueryParam("destination") String destinationId,
                              @QueryParam("departureTime") String departureTimeRaw,
                              @QueryParam("departureDate") String departureDateRaw,
                              @QueryParam("maxChanges") int maxChanges,
                              @QueryParam("maxDuration") int maxDuration) {
        logger.info(format("Query for quicktimes to %s for grid of size %s at %s %s maxchanges %s max duration %s",
                destinationId, gridSize, departureTimeRaw, departureDateRaw, maxChanges, maxDuration));
        Station destination = repository.getStation(destinationId);

        Optional<TramTime> maybeDepartureTime = TramTime.parse(departureTimeRaw);
        if (maybeDepartureTime.isEmpty()) {
            logger.error("Could not parse departure time '" + departureTimeRaw + "'");
            return Response.serverError().build();
        }

        TramTime queryTime = maybeDepartureTime.get();
        LocalDate date = LocalDate.parse(departureDateRaw);

        Transaction tx = graphDatabaseService.beginTx();

        JourneyRequest journeyRequest = new JourneyRequest(new TramServiceDate(date), queryTime,
                false, maxChanges, maxDuration);

        logger.info("Create search");
        Stream<BoxWithCostDTO> results = search.
                findForGridSizeAndDestination(tx, destination, gridSize, journeyRequest).
                map(this::transformToDTO);
        logger.info("Creating stream");
        JsonStreamingOutput<BoxWithCostDTO> jsonStreamingOutput = new JsonStreamingOutput<>(tx, results);

        logger.info("returning stream");
        Response.ResponseBuilder responseBuilder = Response.ok(jsonStreamingOutput);
        return responseBuilder.build();

    }

    private BoxWithCostDTO transformToDTO(BoundingBoxWithCost box) {
        try {
            return BoxWithCostDTO.createFrom(coordinateTransforms, box);
        } catch (TransformException exception) {
            throw new RuntimeException("Unable to convert coordinates ", exception);
        }
    }

}
