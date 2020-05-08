package com.tramchester.resources;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.UpdateRecentJourneys;
import com.tramchester.domain.presentation.DTO.JourneyPlanRepresentation;
import com.tramchester.domain.presentation.ProvidesNotes;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.search.JourneyRequest;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.graph.search.RouteCalculatorArriveBy;
import com.tramchester.mappers.JourneysMapper;
import com.tramchester.repository.TransportData;
import com.tramchester.router.ProcessPlanRequest;
import io.dropwizard.jersey.caching.CacheControl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

@Api
@Path("/journey")
@Produces(MediaType.APPLICATION_JSON)
public class JourneyPlannerResource extends UsesRecentCookie implements APIResource {
    private static final Logger logger = LoggerFactory.getLogger(JourneyPlannerResource.class);

    private final ProcessPlanRequest processPlanRequest;
    private final GraphDatabase graphDatabaseService;

    public JourneyPlannerResource(UpdateRecentJourneys updateRecentJourneys,
                                  ObjectMapper objectMapper, GraphDatabase graphDatabaseService,
                                  ProvidesNow providesNow, ProcessPlanRequest processPlanRequest) {
        super(updateRecentJourneys, providesNow, objectMapper);
        this.processPlanRequest = processPlanRequest;
        this.graphDatabaseService = graphDatabaseService;
    }

    @GET
    @Timed
    @ApiOperation(value = "Find quickest route", response = JourneyPlanRepresentation.class)
    @CacheControl(maxAge = 1, maxAgeUnit = TimeUnit.MINUTES)
    public Response quickestRoute(@QueryParam("start") String startId,
                                  @QueryParam("end") String endId,
                                  @QueryParam("departureTime") String departureTimeRaw,
                                  @QueryParam("departureDate") String departureDateRaw,
                                  @QueryParam("lat") @DefaultValue("0") String lat,
                                  @QueryParam("lon") @DefaultValue("0") String lon,
                                  @QueryParam("arriveby") @DefaultValue("false") String arriveByRaw,
                                  @QueryParam("maxChanges") @DefaultValue("9999") String maxChangesRaw,
                                  @CookieParam(StationResource.TRAMCHESTER_RECENT) Cookie cookie){
        logger.info(format("Plan journey from %s to %s at %s on %s arriveBy=%s maxChanges=%s",
                startId, endId, departureTimeRaw, departureDateRaw, arriveByRaw, maxChangesRaw));

        LocalDate date = LocalDate.parse(departureDateRaw);
        TramServiceDate queryDate = new TramServiceDate(date);

        int maxChanges = Integer.parseInt(maxChangesRaw);

        try {
            Optional<TramTime> maybeDepartureTime = TramTime.parse(departureTimeRaw);
            if (maybeDepartureTime.isPresent()) {
                TramTime queryTime = maybeDepartureTime.get();

                boolean arriveBy = Boolean.parseBoolean(arriveByRaw);
                JourneyRequest journeyRequest = new JourneyRequest(queryDate, queryTime, arriveBy, maxChanges);

                JourneyPlanRepresentation planRepresentation;
                try (Transaction tx = graphDatabaseService.beginTx() ) {
                    planRepresentation =  processPlanRequest.directRequest(startId, endId, journeyRequest, lat, lon);
                }

                if (planRepresentation.getJourneys().size()==0) {
                    logger.warn(format("No journeys found from %s to %s at %s on %s", startId, endId,departureTimeRaw, departureDateRaw));
                }

                Response.ResponseBuilder responseBuilder = Response.ok(planRepresentation);
                responseBuilder.cookie(createRecentCookie(cookie, startId, endId));
                return responseBuilder.build();
            }
        } catch(Exception exception) {
            logger.error("Problem processing response", exception);
        }

        return Response.serverError().build();
    }

}
