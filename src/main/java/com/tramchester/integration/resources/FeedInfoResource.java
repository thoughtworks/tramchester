package com.tramchester.integration.resources;

import com.codahale.metrics.annotation.Timed;
import com.tramchester.domain.FeedInfo;
import com.tramchester.integration.repository.TransportDataFromFiles;
import io.dropwizard.jersey.caching.CacheControl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.concurrent.TimeUnit;

@Api
@Path("/feedinfo")
@Produces(MediaType.APPLICATION_JSON)
public class FeedInfoResource {

    private FeedInfo feedInfo;

    public FeedInfoResource(TransportDataFromFiles dataFromFiles) {
        feedInfo = dataFromFiles.getFeedInfo();
    }

    @GET
    @Timed
    @ApiOperation(value = "Information about version of the data",
            notes = "Extracted from the feed_info.txt file provided by tfgm",
            response = FeedInfo.class)
    @CacheControl(maxAge = 1, maxAgeUnit = TimeUnit.HOURS)
    public Response get() {

        return Response.ok(feedInfo).build();
    }
}
