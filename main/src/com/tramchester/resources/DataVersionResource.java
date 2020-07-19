package com.tramchester.resources;

import com.codahale.metrics.annotation.Timed;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.FeedInfo;
import com.tramchester.domain.presentation.DataVersionDTO;
import com.tramchester.repository.ProvidesFeedInfo;
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
@Path("/datainfo")
@Produces(MediaType.APPLICATION_JSON)
public class DataVersionResource implements APIResource {

    private final TramchesterConfig config;
    private final ProvidesFeedInfo providesFeedInfo;

    public DataVersionResource(TramchesterConfig config, ProvidesFeedInfo providesFeedInfo) {
        this.config = config;
        this.providesFeedInfo = providesFeedInfo;
    }

    @GET
    @Timed
    @ApiOperation(value = "Information about version of the data",
            notes = "Partially Extracted from the feed_info.txt file for a data source",
            response = FeedInfo.class)
    @CacheControl(maxAge = 5, maxAgeUnit = TimeUnit.MINUTES)
    public Response get() {
        FeedInfo original = providesFeedInfo.getFeedInfo();
        DataVersionDTO dataVersionDTO = new DataVersionDTO(original, config);
        return Response.ok(dataVersionDTO).build();
    }
}
