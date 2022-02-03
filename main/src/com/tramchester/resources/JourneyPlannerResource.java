package com.tramchester.resources;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.RedirectToHttpsUsingELBProtoHeader;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.UpdateRecentJourneys;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.domain.presentation.DTO.JourneyPlanRepresentation;
import com.tramchester.domain.presentation.DTO.JourneyQueryDTO;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.mappers.JourneyDTODuplicateFilter;
import com.tramchester.mappers.JourneyToDTOMapper;
import com.tramchester.repository.LocationRepository;
import io.dropwizard.jersey.caching.CacheControl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;

@Api
@Path("/journey")
@Produces(MediaType.APPLICATION_JSON)
public class JourneyPlannerResource extends UsesRecentCookie implements APIResource, GraphDatabaseDependencyMarker {
    private static final Logger logger = LoggerFactory.getLogger(JourneyPlannerResource.class);

    private final LocationJourneyPlanner locToLocPlanner;
    private final JourneyToDTOMapper journeyToDTOMapper;
    private final GraphDatabase graphDatabaseService;
    private final TramchesterConfig config;
    private final JourneyDTODuplicateFilter duplicateFilter;
    private final LocationRepository locationRepository;

    @Inject
    public JourneyPlannerResource(UpdateRecentJourneys updateRecentJourneys,
                                  ObjectMapper objectMapper, GraphDatabase graphDatabaseService,
                                  ProvidesNow providesNow, LocationJourneyPlanner locToLocPlanner, JourneyToDTOMapper journeyToDTOMapper, TramchesterConfig config,
                                  JourneyDTODuplicateFilter duplicateFilter, LocationRepository locationRepository) {
        super(updateRecentJourneys, providesNow, objectMapper);
        this.locToLocPlanner = locToLocPlanner;
        this.journeyToDTOMapper = journeyToDTOMapper;
        this.duplicateFilter = duplicateFilter;
        this.locationRepository = locationRepository;
        this.graphDatabaseService = graphDatabaseService;
        this.config = config;
    }

    // Content-Type header in the POST request with a value of application/json
    @POST
    @Timed
    @ApiOperation(value = "Find quickest route", response = JourneyPlanRepresentation.class)
    @CacheControl(maxAge = 1, maxAgeUnit = TimeUnit.MINUTES)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response quickestRoutePost(JourneyQueryDTO query,
                                      @CookieParam(StationResource.TRAMCHESTER_RECENT) Cookie cookie,
                                      @HeaderParam(RedirectToHttpsUsingELBProtoHeader.X_FORWARDED_PROTO) String forwardedHeader,
                                      @Context UriInfo uriInfo)
    {
        logger.info("Got journey query " + query);

        if (query==null) {
            logger.warn("Got null query");
            return Response.serverError().build();
        }

        if (!query.valid()) {
            logger.error("Problem with received: " + query);
            return Response.serverError().build();
        }

        Location<?> start = locationRepository.getLocation(query.getStartType(), query.getStartId());
        Location<?> dest = locationRepository.getLocation(query.getDestType(), query.getDestId());

        try(Transaction tx = graphDatabaseService.beginTx() ) {

            Stream<JourneyDTO> dtoStream = getJourneyDTOStream(tx, query.getDate(), query.getTime(), start, dest, query.isArriveBy(),
                    query.getMaxChanges());

            // duplicates where same path and timings, just different change points
            Set<JourneyDTO> journeyDTOS = dtoStream.collect(Collectors.toSet());
            Set<JourneyDTO> filtered = duplicateFilter.apply(journeyDTOS);
            int diff = journeyDTOS.size()-filtered.size();
            if (diff!=0) {
                logger.info(format("Filtered out %s of %s journeys", diff, journeyDTOS.size()));
            }

            JourneyPlanRepresentation planRepresentation = new JourneyPlanRepresentation(filtered);
            dtoStream.close();

            if (planRepresentation.getJourneys().size()==0) {
                logger.warn(format("No journeys found from %s to %s at %s on %s", start, dest ,query.getTime(), query.getDate()));
            }

            boolean secure = isHttps(forwardedHeader);

            return buildResponse(Response.ok(planRepresentation), start, dest, cookie, uriInfo, secure);

        } catch(Exception exception) {
            logger.error("Problem processing response", exception);
            return Response.serverError().build();
        }
    }

    @POST
    @Timed
    @Path("/streamed")
    @ApiOperation(value = "Find quickest route", response = JourneyDTO.class)
    @CacheControl(maxAge = 1, maxAgeUnit = TimeUnit.MINUTES)
    @Produces(MediaType.APPLICATION_JSON)
    public Response quickestRouteStream(JourneyQueryDTO query,
                                        @CookieParam(StationResource.TRAMCHESTER_RECENT) Cookie cookie,
                                        @HeaderParam(RedirectToHttpsUsingELBProtoHeader.X_FORWARDED_PROTO) String forwardedHeader,
                                        @Context UriInfo uriInfo) {
        logger.info("Got journey query " + query);

        if (query==null) {
            logger.warn("Got null query");
            return Response.serverError().build();
        }

        if (!query.valid()) {
            logger.error("Problem with received: " + query);
            return Response.serverError().build();
        }

        Location<?> start = locationRepository.getLocation(query.getStartType(), query.getStartId());
        Location<?> dest = locationRepository.getLocation(query.getDestType(), query.getDestId());

        Transaction tx = graphDatabaseService.beginTx();

        try {
            Stream<JourneyDTO> dtoStream = getJourneyDTOStream(tx, query.getDate(), query.getTime(), start, dest, query.isArriveBy(),
                    query.getMaxChanges());

            JsonStreamingOutput<JourneyDTO> jsonStreamingOutput = new JsonStreamingOutput<>(tx, dtoStream, super.mapper);

            boolean secure = isHttps(forwardedHeader);
            return buildResponse(Response.ok(jsonStreamingOutput), start, dest, cookie, uriInfo, secure);

        } catch(Exception exception) {
            logger.error("Problem processing response", exception);
            return Response.serverError().build();
        }
    }

    private Response buildResponse(Response.ResponseBuilder responseBuilder, Location<?> start, Location<?> dest, Cookie cookie,
                                   UriInfo uriInfo, boolean secure) throws JsonProcessingException {
        URI baseUri = uriInfo.getBaseUri();
        responseBuilder.cookie(createRecentCookie(cookie, start, dest, secure, baseUri));
        return responseBuilder.build();
    }


    private Stream<JourneyDTO> getJourneyDTOStream(Transaction tx, LocalDate date, LocalTime time, Location<?> start,
                                                   Location<?> dest, boolean arriveBy, int maxChanges) {

        TramTime queryTime = TramTime.of(time);
        JourneyRequest journeyRequest = new JourneyRequest(date, queryTime, arriveBy, maxChanges,
                config.getMaxJourneyDuration(),  config.getMaxNumResults());

        logger.info(format("Plan journey from %s to %s on %s", start, dest, journeyRequest));

        return locToLocPlanner.quickestRouteForLocation(tx, start, dest, journeyRequest).
                filter(journey -> !journey.getStages().isEmpty()).
                map(journey -> journeyToDTOMapper.createJourneyDTO(journey, journeyRequest.getDate()));

    }


}
