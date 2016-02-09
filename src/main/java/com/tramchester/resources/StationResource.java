package com.tramchester.resources;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.ClosedStations;
import com.tramchester.domain.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.StationClosureMessage;
import com.tramchester.repository.TransportDataFromFiles;
import com.tramchester.services.SpatialService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.String.format;

@Path("/stations")
@Produces(MediaType.APPLICATION_JSON)
public class StationResource {
    private static final Logger logger = LoggerFactory.getLogger(StationResource.class);

    private final Map<String,Station> workingStations;
    private final SpatialService spatialService;
    private final ClosedStations closedStations;
    private final TramchesterConfig config;
    private final ObjectMapper mapper;

    public StationResource(TransportDataFromFiles transportData, SpatialService spatialService,
                           ClosedStations closedStations, TramchesterConfig config) {
        this.spatialService = spatialService;
        this.closedStations = closedStations;
        this.config = config;
        this.workingStations = transportData.getStations().stream()
                .filter(station -> !closedStations.contains(station.getName()))
                .collect(Collectors.<Station, String, Station>toMap(s -> s.getId(), s->s));
        mapper = new ObjectMapper();
    }

    @GET
    @Timed
    public Response getAll() {
        logger.info("Get all stations");
        return Response.ok(mapStations()).build();
    }

    // TODO SORT ON WAY IN
    private List<Station> mapStations() {
        List<Station> sorted = workingStations.values().stream()
                .sorted((s1, s2) -> s1.getName().compareTo(s2.getName())).collect(Collectors.toList());
        sorted.forEach(station -> station.setProximityGroup("All Stops"));
        return sorted;
    }

    @GET
    @Timed
    @Path("/closures")
    public Response getClosures() {
        logger.info("Get station closures");
        StationClosureMessage stationClosureMessage = new StationClosureMessage(closedStations);
        return Response.ok(stationClosureMessage).build();
    }

    @GET
    @Path("/{id}")
    public Response get(@PathParam("id") String id) {
        logger.info("Get station " + id);
        if (workingStations.containsKey(id)) {
            return Response.ok(workingStations.get(id)).build();
        }
        return Response.status(Response.Status.NOT_FOUND).build();
    }

    @GET
    @Path("/{lat}/{lon}")
    public Response getNearest(@PathParam("lat") double lat, @PathParam("lon") double lon) throws JsonProcessingException {
        logger.info(format("Get station at %s,%s ", lat, lon));

        List<Station> orderedStations = spatialService.reorderNearestStations(new LatLong(lat, lon), workingStations);

        if (config.showMyLocation()) {
            Station myLocation = new Station(formId(lat,lon), "", "My Location", lat, lon, false);
            myLocation.setProximityGroup("Nearby");
            orderedStations.add(0, myLocation);
        }

        return Response.ok(orderedStations).build();
    }

    private String formId(double lat, double lon) throws JsonProcessingException {
        return mapper.writeValueAsString(new LatLong(lat, lon));
    }
}
