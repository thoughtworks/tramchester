package com.tramchester.resources;


import com.codahale.metrics.annotation.Timed;
import com.tramchester.dataimport.TransportDataImporter;
import com.tramchester.domain.TransportData;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class TestResource {


    @GET
    @Timed
    public Response get() {

        TransportData transportData = new TransportDataImporter().load();
        return Response.ok(transportData).build();


    }
}


