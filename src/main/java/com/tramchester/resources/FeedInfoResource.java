package com.tramchester.resources;

import com.codahale.metrics.annotation.Timed;
import com.tramchester.domain.FeedInfo;
import com.tramchester.repository.TransportDataFromFiles;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/feedinfo")
@Produces(MediaType.APPLICATION_JSON)
public class FeedInfoResource {

    private FeedInfo feedInfo;

    public FeedInfoResource(TransportDataFromFiles dataFromFiles) {
        feedInfo = dataFromFiles.getFeedInfo();
    }

    @GET
    @Timed
    public Response get() {

        return Response.ok(feedInfo).build();
    }
}
