package com.tramchester.resources;

import com.codahale.metrics.annotation.Timed;
import com.tramchester.domain.Station;
import com.tramchester.domain.TransportData;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.SQLException;
import java.util.List;

@Path("/stations")
@Produces(MediaType.APPLICATION_JSON)
public class StationResource {
    private final List<Station> stations;

    public StationResource(TransportData transportData) {
        this.stations = transportData.getStations();
    }

    @GET
    @Timed
    public Response getAll() throws SQLException {
        return Response.ok(stations).build();
    }

    @GET
    @Path("/{id}")
    public Response get(@PathParam("id") String id) {

        for (Station station : stations) {
            if (station.getId().equals(id)) {
                return Response.ok(station).build();
            }
        }

        return Response.status(Response.Status.NOT_FOUND).build();
    }
}
