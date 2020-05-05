package com.tramchester.resources;


import com.codahale.metrics.annotation.Timed;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.Note;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.domain.liveUpdates.DueTram;
import com.tramchester.domain.presentation.DTO.DepartureDTO;
import com.tramchester.domain.presentation.DTO.DepartureListDTO;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.ProvidesNotes;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.mappers.DeparturesMapper;
import com.tramchester.repository.LiveDataSource;
import com.tramchester.repository.StationRepository;
import com.tramchester.services.SpatialService;
import io.dropwizard.jersey.caching.CacheControl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

@Api
@Path("/departures")
@Produces(MediaType.APPLICATION_JSON)
public class DeparturesResource implements APIResource  {
    private static final Logger logger = LoggerFactory.getLogger(DeparturesResource.class);

    private final SpatialService spatialService;
    private final LiveDataSource liveDataSource;
    private final DeparturesMapper departuresMapper;
    private final ProvidesNotes providesNotes;
    private final StationRepository stationRepository;
    private final ProvidesNow providesNow;

    public DeparturesResource(SpatialService spatialService, LiveDataSource liveDataSource,
                              DeparturesMapper departuresMapper, ProvidesNotes providesNotes, StationRepository stationRepository, ProvidesNow providesNow) {
        this.spatialService = spatialService;
        this.liveDataSource = liveDataSource;
        this.departuresMapper = departuresMapper;
        this.providesNotes = providesNotes;
        this.stationRepository = stationRepository;
        this.providesNow = providesNow;
    }

    @GET
    @Timed
    @Path("/{lat}/{lon}")
    @ApiOperation(value = "Get geographically close departures", response = DepartureListDTO.class)
    @CacheControl(maxAge = 30, maxAgeUnit = TimeUnit.SECONDS)
    public Response getNearestDepartures(@PathParam("lat") double lat, @PathParam("lon") double lon,
                                         @DefaultValue("") @QueryParam("querytime") String queryTimeRaw) {
        LatLong latLong = new LatLong(lat,lon);
        List<Station> nearbyStations = spatialService.getNearestStations(latLong);

        TramServiceDate queryDate = new TramServiceDate(providesNow.getDate());

        Optional<TramTime> optionalTramTime = Optional.empty();
        if (!queryTimeRaw.isEmpty()) {
            optionalTramTime = TramTime.parse(queryTimeRaw);
            if (optionalTramTime.isEmpty()) {
                logger.warn("Unable to parse time " + queryTimeRaw);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }
        }
        TramTime queryTime = optionalTramTime.orElseGet(providesNow::getNow);

        // trams
        SortedSet<DepartureDTO> departs = new TreeSet<>();
        nearbyStations.forEach(station -> {
            List<DueTram> dueTrams = liveDataSource.dueTramsFor(station, queryDate, queryTime);
            departs.addAll(departuresMapper.mapToDTO(station, dueTrams));
        });

        // notes
        List<Note> notes = providesNotes.createNotesForStations(nearbyStations, queryDate, queryTime);

        return Response.ok(new DepartureListDTO(departs, notes)).build();
    }

    @GET
    @Timed
    @Path("/station/{station}")
    @ApiOperation(value= "All departures from given station ID, notes included by default. " +
            "Control presence of notes using query param ?notes=1 or 0",
            response = DepartureListDTO.class)
    @CacheControl(maxAge = 30, maxAgeUnit = TimeUnit.SECONDS)
    public Response getDepartureForStation(@PathParam("station") String stationId,
                                           @DefaultValue("1") @QueryParam("notes") String notesParam,
                                           @DefaultValue("") @QueryParam("querytime") String queryTimeRaw) {

        logger.info(format("Get departs for station %s at %s with notes %s ", stationId, queryTimeRaw, notesParam));
        if (!stationRepository.hasStationId(stationId)) {
            logger.warn("Unable to find station " + stationId);
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        boolean includeNotes = true;
        if (!notesParam.isEmpty()) {
            includeNotes = !notesParam.equals("0");
        }

        TramServiceDate queryDate = new TramServiceDate(providesNow.getDate());

        Optional<TramTime> optionalTramTime = Optional.empty();
        if (!queryTimeRaw.isEmpty()) {
            optionalTramTime = TramTime.parse(queryTimeRaw);
            if (optionalTramTime.isEmpty()) {
                logger.warn("Unable to parse time " + queryTimeRaw);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }
        }
        TramTime queryTime = optionalTramTime.orElseGet(providesNow::getNow);

        logger.info("Found station, now find departures");
        Station station = stationRepository.getStation(stationId);

        //trams
        List<DueTram> dueTramList = liveDataSource.dueTramsFor(station, queryDate, queryTime);
        SortedSet<DepartureDTO> dueTrams = new TreeSet<>(departuresMapper.mapToDTO(station, dueTramList));

        //notes
        List<Note> notes = Collections.emptyList();
        if (includeNotes) {
            notes = providesNotes.createNotesForStations(Collections.singletonList(station), queryDate, queryTime);
        }

        return Response.ok(new DepartureListDTO(dueTrams, notes)).build();

    }
}
