package com.tramchester.resources;

import com.codahale.metrics.annotation.Timed;
import com.tramchester.domain.ClosedStations;
import com.tramchester.domain.Station;
import com.tramchester.domain.StationClosureMessage;
import com.tramchester.domain.TransportDataFromFiles;
import com.tramchester.services.SpatialService;

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

@Path("/stations")
@Produces(MediaType.APPLICATION_JSON)
public class StationResource {
    private final Map<String,Station> workingStations;
    private final SpatialService spatialService;
    private final ClosedStations closedStations;

    public StationResource(TransportDataFromFiles transportData, SpatialService spatialService, ClosedStations closedStations) {
        this.spatialService = spatialService;
        this.closedStations = closedStations;
        this.workingStations = transportData.getStations().stream()
                .filter(station -> !closedStations.contains(station.getName()))
                .collect(Collectors.<Station, String, Station>toMap(s -> s.getId(), s->s));
    }

    @GET
    @Timed
    public Response getAll() throws SQLException {
        return Response.ok(workingStations.values()).build();
    }

    @GET
    @Timed
    @Path("/closures")
    public Response getClosures() {
        StationClosureMessage stationClosureMessage = new StationClosureMessage(closedStations);
        return Response.ok(stationClosureMessage).build();
    }

    @GET
    @Path("/{id}")
    public Response get(@PathParam("id") String id) {

        if (workingStations.containsKey(id)) {
            return Response.ok(workingStations.get(id)).build();
        }
//        for (Station station : workingStations) {
//            if (station.getId().equals(id)) {
//                return Response.ok(station).build();
//            }
//        }

        return Response.status(Response.Status.NOT_FOUND).build();
    }

    @GET
    @Path("/{lat}/{lon}")
    public Response getNearest(@PathParam("lat") double lat, @PathParam("lon") double lon) {
        List<Station> orderedStations = spatialService.reorderNearestStations(lat, lon, workingStations);
        return Response.ok(orderedStations).build();
    }
}
