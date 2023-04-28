package com.tramchester.resources;


import com.codahale.metrics.annotation.Timed;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.DeparturesQueryDTO;
import com.tramchester.domain.presentation.Note;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.livedata.domain.DTO.DepartureDTO;
import com.tramchester.livedata.domain.DTO.DepartureListDTO;
import com.tramchester.livedata.domain.liveUpdates.UpcomingDeparture;
import com.tramchester.livedata.mappers.DeparturesMapper;
import com.tramchester.livedata.repository.DeparturesRepository;
import com.tramchester.livedata.repository.ProvidesNotes;
import com.tramchester.livedata.tfgm.ProvidesTramNotes;
import com.tramchester.repository.LocationRepository;
import io.dropwizard.jersey.caching.CacheControl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Path("/departures")
@Produces(MediaType.APPLICATION_JSON)
public class DeparturesResource extends TransportResource implements APIResource {
    private static final Logger logger = LoggerFactory.getLogger(DeparturesResource.class);

    private final LocationRepository locationRepository;
    private final DeparturesMapper departuresMapper;
    private final DeparturesRepository departuresRepository;
    private final ProvidesNotes providesNotes;
    private final TramchesterConfig config;

    @Inject
    public DeparturesResource(LocationRepository locationRepository,
                              DeparturesMapper departuresMapper, DeparturesRepository departuresRepository,
                              ProvidesTramNotes providesNotes,
                              ProvidesNow providesNow, TramchesterConfig config) {
        super(providesNow);
        this.locationRepository = locationRepository;
        this.departuresMapper = departuresMapper;
        this.departuresRepository = departuresRepository;
        this.providesNotes = providesNotes;
        this.config = config;
        logger.info("created");
    }

    @POST
    @Timed
    @Path("/location")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Get departures for a location")
    @ApiResponse(content = @Content(schema = @Schema(implementation = DepartureListDTO.class)))
    @CacheControl(maxAge = 30, maxAgeUnit = TimeUnit.SECONDS)
    public Response getNearestDepartures(DeparturesQueryDTO departuresQuery) {

        if (departuresQuery.getLocationType()==null || departuresQuery.getLocationId()==null) {
            logger.error("Cannot process departure query: " + departuresQuery);
            return Response.serverError().build();
        }

        logger.info("Get departures for " + departuresQuery);

        Location<?> location = locationRepository.getLocation(departuresQuery.getLocationType(),
                departuresQuery.getLocationId());

        LocalDateTime dateTime = providesNow.getDateTime();
        TramDate queryDate = TramDate.from(dateTime);

        TramTime queryTime;
        if (departuresQuery.hasValidTime()) {
            queryTime = TramTime.ofHourMins(departuresQuery.getTime());
        } else {
            queryTime = providesNow.getNowHourMins();
        }

        EnumSet<TransportMode> modes = departuresQuery.getModes();
        if (modes.isEmpty()) {
            logger.warn("modes not supplied, fall back to all configured modes");
            modes = config.getTransportModes();
        }

        List<UpcomingDeparture> dueTrams = departuresRepository.dueTramsForLocation(location, dateTime.toLocalDate(), queryTime, modes);
        if (dueTrams.isEmpty()) {
            logger.warn("Departures list empty for " + location.getId() + " at " + queryTime);
        }
        SortedSet<DepartureDTO> departs = new TreeSet<>(departuresMapper.mapToDTO(dueTrams, providesNow.getDateTime()));

        List<Note> notes = Collections.emptyList();
        if (departuresQuery.getIncludeNotes()) {
            List<Station> nearbyStations = dueTrams.stream().
                    map(UpcomingDeparture::getDisplayLocation).
                    distinct().collect(Collectors.toList());
            notes = providesNotes.createNotesForStations(nearbyStations, queryDate, queryTime);
            if (notes.isEmpty()) {
                logger.warn("Notes empty for " + location.getId() + " at " + queryTime);
            }
        }

        return Response.ok(new DepartureListDTO(departs, notes)).build();
    }



}
