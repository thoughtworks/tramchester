package com.tramchester.resources;


import com.codahale.metrics.annotation.Timed;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.HasId;
import com.tramchester.domain.IdFor;
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
import com.tramchester.geo.StationLocations;
import com.tramchester.mappers.DeparturesMapper;
import com.tramchester.repository.DueTramsSource;
import com.tramchester.repository.StationRepository;
import io.dropwizard.jersey.caching.CacheControl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

@Api
@Path("/departures")
@Produces(MediaType.APPLICATION_JSON)
public class DeparturesResource implements APIResource  {
    private static final Logger logger = LoggerFactory.getLogger(DeparturesResource.class);

    private final StationLocations stationLocations;
    private final DueTramsSource dueTramsSource;
    private final DeparturesMapper departuresMapper;
    private final ProvidesNotes providesNotes;
    private final StationRepository stationRepository;
    private final ProvidesNow providesNow;
    private final TramchesterConfig config;

    @Inject
    public DeparturesResource(StationLocations stationLocations, DueTramsSource dueTramsSource,
                              DeparturesMapper departuresMapper, ProvidesNotes providesNotes, StationRepository stationRepository,
                              ProvidesNow providesNow, TramchesterConfig config) {
        this.stationLocations = stationLocations;
        this.dueTramsSource = dueTramsSource;
        this.departuresMapper = departuresMapper;
        this.providesNotes = providesNotes;
        this.stationRepository = stationRepository;
        this.providesNow = providesNow;
        this.config = config;
    }

    @GET
    @Timed
    @Path("/{lat}/{lon}")
    @ApiOperation(value = "Get geographically close departures", response = DepartureListDTO.class)
    @CacheControl(maxAge = 30, maxAgeUnit = TimeUnit.SECONDS)
    public Response getNearestDepartures(@PathParam("lat") double lat, @PathParam("lon") double lon,
                                         @DefaultValue("") @QueryParam("querytime") String queryTimeRaw) {
        LatLong latLong = new LatLong(lat,lon);
        List<Station> nearbyStations = stationLocations.getNearestStationsTo(latLong,
                config.getNumOfNearestStopsToOffer(), config.getNearestStopRangeKM());

        LocalDate localDate = providesNow.getDate();
        TramServiceDate queryDate = new TramServiceDate(localDate);

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
            List<DueTram> dueTrams = dueTramsSource.dueTramsFor(station, localDate, queryTime);
            departs.addAll(departuresMapper.mapToDTO(station, dueTrams, localDate));
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
    public Response getDepartureForStation(@PathParam("station") String stationIdText,
                                           @DefaultValue("1") @QueryParam("notes") String notesParam,
                                           @DefaultValue("") @QueryParam("querytime") String queryTimeRaw) {

        IdFor<Station> stationId = IdFor.createId(stationIdText);
        logger.info(format("Get departs for station %s at '%s' with notes enabled:'%s'", stationId, queryTimeRaw, notesParam));
        if (!stationRepository.hasStationId(stationId)) {
            logger.warn("Unable to find station " + stationId);
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        boolean includeNotes = true;
        if (!notesParam.isEmpty()) {
            includeNotes = !notesParam.equals("0");
        }

        LocalDate localDate = providesNow.getDate();
        TramServiceDate queryDate = new TramServiceDate(localDate);

        Optional<TramTime> optionalTramTime = Optional.empty();
        if (!queryTimeRaw.isEmpty()) {
            optionalTramTime = TramTime.parse(queryTimeRaw);
            if (optionalTramTime.isEmpty()) {
                logger.warn("Unable to parse time " + queryTimeRaw);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }
        }
        TramTime queryTime = optionalTramTime.orElseGet(providesNow::getNow);

        Station station = stationRepository.getStationById(stationId);
        logger.info("Found station " + HasId.asId(station) + " Find departures at " + queryDate + " " +queryTime);

        //trams
        List<DueTram> dueTramList = dueTramsSource.dueTramsFor(station, localDate, queryTime);
        if (dueTramList.isEmpty()) {
            logger.warn("Departures list empty for " + HasId.asId(station) + " at " + queryDate + " " +queryTime);
        }
        SortedSet<DepartureDTO> dueTrams = new TreeSet<>(departuresMapper.mapToDTO(station, dueTramList, localDate));

        //notes
        List<Note> notes = Collections.emptyList();
        if (includeNotes) {
            notes = providesNotes.createNotesForStations(Collections.singletonList(station), queryDate, queryTime);
            if (notes.isEmpty()) {
                logger.warn("Notes empty for " + HasId.asId(station) + " at " + queryDate + " " +queryTime);
            }
        }

        return Response.ok(new DepartureListDTO(dueTrams, notes)).build();

    }
}
