package com.tramchester.resources;


import com.codahale.metrics.annotation.Timed;
import com.tramchester.domain.Station;
import com.tramchester.domain.liveUpdates.StationDepartureInfo;
import com.tramchester.domain.presentation.DTO.DepartureDTO;
import com.tramchester.domain.presentation.DTO.DepartureListDTO;
import com.tramchester.domain.presentation.DTO.StationDTO;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.ProvidesNotes;
import com.tramchester.mappers.DeparturesMapper;
import com.tramchester.repository.LiveDataRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.services.SpatialService;
import io.dropwizard.jersey.caching.CacheControl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Api
@Path("/departures")
@Produces(MediaType.APPLICATION_JSON)
public class DeparturesResource {

    private final SpatialService spatialService;
    private final LiveDataRepository liveDataRepository;
    private final DeparturesMapper departuresMapper;
    private ProvidesNotes providesNotes;
    private final DateTimeZone timeZone;
    private final StationRepository stationRepository;

    public DeparturesResource(SpatialService spatialService, LiveDataRepository liveDataRepository,
                              DeparturesMapper departuresMapper, ProvidesNotes providesNotes, StationRepository stationRepository) {
        this.spatialService = spatialService;
        this.liveDataRepository = liveDataRepository;
        this.departuresMapper = departuresMapper;
        this.providesNotes = providesNotes;
        this.stationRepository = stationRepository;
        timeZone = DateTimeZone.forTimeZone(TimeZone.getTimeZone("Europe/London"));
    }

    @GET
    @Timed
    @Path("/{lat}/{lon}")
    @ApiOperation(value = "Get geographically close departures", response = DepartureListDTO.class)
    @CacheControl(maxAge = 30, maxAgeUnit = TimeUnit.SECONDS)
    public Response getNearestDepartures(@PathParam("lat") double lat, @PathParam("lon") double lon) {
        DateTime time = DateTime.now(timeZone);

        LatLong latLong = new LatLong(lat,lon);
        List<StationDTO> nearbyStations = spatialService.getNearestStations(latLong);

        nearbyStations.forEach(station -> liveDataRepository.enrich(station, time));

        SortedSet<DepartureDTO> departuresDTO = departuresMapper.fromStations(nearbyStations);
        List<String> notes = providesNotes.createNotesForStations(nearbyStations);
        DepartureListDTO departureList = new DepartureListDTO(departuresDTO, notes);

        return Response.ok(departureList).build();
    }

    @GET
    @Timed
    @Path("/station/{station}")
    @ApiOperation(value= "All departures from given station ID, notes included by default. " +
            "Control presence of notes using query param ?notes=1 or 0",
            response = DepartureListDTO.class)
    @CacheControl(maxAge = 30, maxAgeUnit = TimeUnit.SECONDS)
    public Response getDepartureForStation(@PathParam("station") String stationId,
                                           @DefaultValue("1") @QueryParam("notes") String notesParam) {

        boolean includeNotes = true;
        if (!notesParam.isEmpty()) {
            includeNotes = !notesParam.equals("0");
        }

        Optional<Station> maybeStation = stationRepository.getStation(stationId);

        if (maybeStation.isPresent()) {
            Station station = maybeStation.get();
            List<StationDepartureInfo> departs = liveDataRepository.departuresFor(station);

            DepartureListDTO departureList = departuresMapper.from(departs,includeNotes);

            return Response.ok(departureList).build();
        }

        return Response.status(Response.Status.NOT_FOUND).build();

    }
}
