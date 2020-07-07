package com.tramchester.resources;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.RedirectToHttpsUsingELBProtoHeader;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.UpdateRecentJourneys;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.domain.presentation.DTO.JourneyPlanRepresentation;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.search.JourneyRequest;
import com.tramchester.router.ProcessPlanRequest;
import io.dropwizard.jersey.caching.CacheControl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.jetbrains.annotations.NotNull;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;

@Api
@Path("/journey")
@Produces(MediaType.APPLICATION_JSON)
public class JourneyPlannerResource extends UsesRecentCookie implements APIResource {
    private static final Logger logger = LoggerFactory.getLogger(JourneyPlannerResource.class);

    private final ProcessPlanRequest processPlanRequest;
    private final GraphDatabase graphDatabaseService;
    private final TramchesterConfig config;

    public JourneyPlannerResource(UpdateRecentJourneys updateRecentJourneys,
                                  ObjectMapper objectMapper, GraphDatabase graphDatabaseService,
                                  ProvidesNow providesNow, ProcessPlanRequest processPlanRequest, TramchesterConfig config) {
        super(updateRecentJourneys, providesNow, objectMapper);
        this.processPlanRequest = processPlanRequest;
        this.graphDatabaseService = graphDatabaseService;
        this.config = config;
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
                                  @CookieParam(StationResource.TRAMCHESTER_RECENT) Cookie cookie,
                                  @HeaderParam(RedirectToHttpsUsingELBProtoHeader.X_FORWARDED_PROTO) String forwardedHeader,
                                  @Context UriInfo uriInfo) {
        logger.info(format("Plan journey from %s to %s at %s on %s arriveBy=%s maxChanges=%s",
                startId, endId, departureTimeRaw, departureDateRaw, arriveByRaw, maxChangesRaw));

        Optional<TramTime> maybeDepartureTime = TramTime.parse(departureTimeRaw);
        if (maybeDepartureTime.isEmpty()) {
            logger.error("Could not parse departure time '" + departureTimeRaw +"'");
            return Response.serverError().build();
        }
        TramTime queryTime = maybeDepartureTime.get();

        boolean secure = forwardedHeader != null && forwardedHeader.toLowerCase().equals("https");
        URI baseUri = uriInfo.getBaseUri();

        try(Transaction tx = graphDatabaseService.beginTx() ) {
            JourneyRequest journeyRequest = createJourneyRequest(departureDateRaw, arriveByRaw, maxChangesRaw,
                    queryTime, config.getMaxJourneyDuration());

            Stream<JourneyDTO> dtoStream = processPlanRequest.directRequest(tx, startId, endId, journeyRequest, lat, lon);
            JourneyPlanRepresentation planRepresentation = new JourneyPlanRepresentation(dtoStream.collect(Collectors.toSet()));
            dtoStream.close();

            if (planRepresentation.getJourneys().size()==0) {
                logger.warn(format("No journeys found from %s to %s at %s on %s", startId, endId,departureTimeRaw, departureDateRaw));
            }

            Response.ResponseBuilder responseBuilder = Response.ok(planRepresentation);
            responseBuilder.cookie(createRecentCookie(cookie, startId, endId, secure, baseUri));
            return responseBuilder.build();

        } catch(Exception exception) {
            logger.error("Problem processing response", exception);
            return Response.serverError().build();
        }
    }

    @GET
    @Timed
    @Path("/streamed")
    @ApiOperation(value = "Find quickest route", response = JourneyDTO.class)
    @CacheControl(maxAge = 1, maxAgeUnit = TimeUnit.MINUTES)
    @Produces(MediaType.APPLICATION_JSON)
    public Response quickestRouteStream(@QueryParam("start") String startId,
                                  @QueryParam("end") String endId,
                                  @QueryParam("departureTime") String departureTimeRaw,
                                  @QueryParam("departureDate") String departureDateRaw,
                                  @QueryParam("lat") @DefaultValue("0") String lat,
                                  @QueryParam("lon") @DefaultValue("0") String lon,
                                  @QueryParam("arriveby") @DefaultValue("false") String arriveByRaw,
                                  @QueryParam("maxChanges") @DefaultValue("9999") String maxChangesRaw,
                                  @CookieParam(StationResource.TRAMCHESTER_RECENT) Cookie cookie,
                                  @HeaderParam(RedirectToHttpsUsingELBProtoHeader.X_FORWARDED_PROTO) String forwardedHeader,
                                  @Context UriInfo uriInfo) {
        logger.info(format("Plan journey from %s to %s at %s on %s arriveBy=%s maxChanges=%s",
                startId, endId, departureTimeRaw, departureDateRaw, arriveByRaw, maxChangesRaw));

        Optional<TramTime> maybeDepartureTime = TramTime.parse(departureTimeRaw);
        if (maybeDepartureTime.isEmpty()) {
            logger.error("Could not parse departure time '" + departureTimeRaw +"'");
            return Response.serverError().build();
        }
        TramTime queryTime = maybeDepartureTime.get();

        boolean secure = forwardedHeader != null && forwardedHeader.toLowerCase().equals("https");
        URI baseUri = uriInfo.getBaseUri();

        Transaction tx = graphDatabaseService.beginTx();

        try {
            JourneyRequest journeyRequest = createJourneyRequest(departureDateRaw, arriveByRaw, maxChangesRaw,
                    queryTime, config.getMaxJourneyDuration());
            Stream<JourneyDTO> dtoStream = processPlanRequest.directRequest(tx, startId, endId, journeyRequest, lat, lon);

            JsonStreamingOutput<JourneyDTO> jsonStreamingOutput = new JsonStreamingOutput<>(tx, dtoStream);

            Response.ResponseBuilder responseBuilder = Response.ok(jsonStreamingOutput);
            responseBuilder.cookie(createRecentCookie(cookie, startId, endId, secure, baseUri));
            return responseBuilder.build();

        } catch(Exception exception) {
            logger.error("Problem processing response", exception);
            return Response.serverError().build();
        }
    }

    @NotNull
    private JourneyRequest createJourneyRequest(String departureDateRaw, String arriveByRaw, String maxChangesRaw,
                                                TramTime queryTime, int maxJourneyDuration) {
        LocalDate date = LocalDate.parse(departureDateRaw);
        TramServiceDate queryDate = new TramServiceDate(date);

        int maxChanges = Integer.parseInt(maxChangesRaw);

        boolean arriveBy = Boolean.parseBoolean(arriveByRaw);
        return new JourneyRequest(queryDate, queryTime, arriveBy, maxChanges, maxJourneyDuration);
    }


}
