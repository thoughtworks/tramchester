package com.tramchester.resources;


import com.codahale.metrics.annotation.Timed;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.Note;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.livedata.domain.liveUpdates.DueTram;
import com.tramchester.livedata.domain.DTO.DepartureDTO;
import com.tramchester.livedata.domain.DTO.DepartureListDTO;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.ProvidesNotes;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.geo.MarginInMeters;
import com.tramchester.geo.StationLocations;
import com.tramchester.geo.StationLocationsRepository;
import com.tramchester.livedata.mappers.DeparturesMapper;
import com.tramchester.livedata.repository.DueTramsSource;
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
public class DeparturesResource extends TransportResource implements APIResource {
    private static final Logger logger = LoggerFactory.getLogger(DeparturesResource.class);

    private final StationLocationsRepository stationLocations;
    private final DueTramsSource dueTramsSource;
    private final DeparturesMapper departuresMapper;
    private final ProvidesNotes providesNotes;
    private final StationRepository stationRepository;
    private final TramchesterConfig config;

    @Inject
    public DeparturesResource(StationLocations stationLocations, DueTramsSource dueTramsSource,
                              DeparturesMapper departuresMapper, ProvidesNotes providesNotes, StationRepository stationRepository,
                              ProvidesNow providesNow, TramchesterConfig config) {
        super(providesNow);
        logger.info("created");
        this.stationLocations = stationLocations;
        this.dueTramsSource = dueTramsSource;
        this.departuresMapper = departuresMapper;
        this.providesNotes = providesNotes;
        this.stationRepository = stationRepository;
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
        MarginInMeters margin = MarginInMeters.of(config.getNearestStopRangeKM());
        logger.info("Get departures within " + margin + " of " + latLong + " at " + queryTimeRaw);

        List<Station> nearbyStations = stationLocations.nearestStationsSorted(latLong,
                config.getNumOfNearestStopsToOffer(), margin);

        LocalDate localDate = providesNow.getDate();
        TramServiceDate queryDate = new TramServiceDate(localDate);

        TramTime queryTime = parseOptionalTimeOrNow(queryTimeRaw);

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
            "Control presence of notes using query param ?notes=1 or 0 (default 0). Uses latest data " +
            "unless queryTime is provided",
            response = DepartureListDTO.class)
    @CacheControl(maxAge = 30, maxAgeUnit = TimeUnit.SECONDS)
    public Response getDepartureForStation(@PathParam("station") String stationIdText,
                                           @DefaultValue("1") @QueryParam("notes") String notesParam,
                                           @DefaultValue("") @QueryParam("querytime") String queryTimeRaw) {

        IdFor<Station> stationId = StringIdFor.createId(stationIdText);
        logger.info(format("Get departs for station %s at '%s' with notes enabled:'%s'", stationId, queryTimeRaw, notesParam));
        guardForStationNotExisting(stationRepository, stationId);

        boolean includeNotes = true;
        if (!notesParam.isEmpty()) {
            includeNotes = !notesParam.equals("0");
        }

        LocalDate currentDate = providesNow.getDate();
        TramTime queryTime = parseOptionalTimeOrNow(queryTimeRaw);

        Station station = stationRepository.getStationById(stationId);
        logger.info("Found station " + HasId.asId(station) + " Find departures at " + currentDate + " " +queryTime);

        //trams
        List<DueTram> dueTramList = dueTramsSource.dueTramsFor(station, currentDate, queryTime);
        if (dueTramList.isEmpty()) {
            logger.warn("Departures list empty for " + HasId.asId(station) + " at " + currentDate + " " +queryTime);
        }
        // not strictly needed, but saves some cycles in the front-end
        SortedSet<DepartureDTO> dueTrams = new TreeSet<>(departuresMapper.mapToDTO(station, dueTramList, currentDate));

        //notes
        List<Note> notes = Collections.emptyList();
        if (includeNotes) {
            notes = providesNotes.createNotesForStations(Collections.singletonList(station),
                    new TramServiceDate(currentDate), queryTime);
            if (notes.isEmpty()) {
                logger.warn("Notes empty for " + HasId.asId(station) + " at " + currentDate + " " +queryTime);
            }
        }

        return Response.ok(new DepartureListDTO(dueTrams, notes)).build();
    }



}
